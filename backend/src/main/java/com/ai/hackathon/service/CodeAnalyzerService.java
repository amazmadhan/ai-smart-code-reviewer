package com.ai.hackathon.service;

import com.ai.hackathon.model.AnalysisResult;
import com.ai.hackathon.model.Issue;
import com.ai.hackathon.util.JavaHeuristics;
import com.ai.hackathon.util.ScoreEngine;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CodeAnalyzerService {

    @Autowired
    private OpenAIClient openAIClient;

    // Add a map to store previously refactored code
    private Map<String, String> previouslyRefactored = new ConcurrentHashMap<>();

    public AnalysisResult analyzeOnly(String fileName, String source) {
        AnalysisResult result = new AnalysisResult();
        result.setFileName(fileName);

        // Check if we have a previous refactoring for this source
        String sourceKey = generateSourceKey(fileName, source);
        String startingSource = source;
        boolean isPreviouslyRefactored = previouslyRefactored.containsKey(sourceKey);

        // If previously refactored, use that version as starting point
        if (isPreviouslyRefactored) {
            startingSource = previouslyRefactored.get(sourceKey);
        }

        result.setOriginalSource(source);

        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> pr = parser.parse(source);
        if (pr.isSuccessful() && pr.getResult().isPresent()) {
            // Process original code as before
            CompilationUnit cu = pr.getResult().get();
            List<Issue> issues = JavaHeuristics.findIssuesWithLines(cu, source);
            result.setIssues(issues);

            int origScore = ScoreEngine.scoreFromIssues(issues);
            origScore = Math.max(origScore, 40);
            result.setOriginalScore(origScore);

            String astRefactored = JavaHeuristics.applyAstRefactors(startingSource);

            StringBuilder prompt = new StringBuilder();
            // Modify prompt to be more aggressive if previously refactored
            if (isPreviouslyRefactored) {
                prompt.append("You are a senior Java refactoring expert. The code needs further improvements to reach a perfect score. ");
                prompt.append("The code was previously refactored but still has these issues:\n");
            } else {
                prompt.append("You are a senior Java reviewer. The file has the following issues:\n");
            }

            for (Issue iss : issues) {
                prompt.append("Line ").append(iss.getLine()).append(": ").append(iss.getMessage()).append("\n");
            }
            prompt.append("\nProvide concise suggestions to fix each issue, and provide a brief overall summary. Reply in plain text.");

            String aiResp = openAIClient.askModel(prompt.toString());
            List<String> suggestions = parseAiSuggestions(aiResp);
            result.setAiSuggestions(suggestions);

            // Apply more aggressive refactoring if previously refactored
            String refactoredSource = applyRefactoringWithAISuggestions(startingSource, suggestions, astRefactored, isPreviouslyRefactored);
            result.setRefactoredSource(refactoredSource);

            // Store this refactored version for future use
            previouslyRefactored.put(sourceKey, refactoredSource);

            // Analyze refactored code
            ParseResult<CompilationUnit> pr2 = parser.parse(refactoredSource);
            if (pr2.isSuccessful() && pr2.getResult().isPresent()) {
                List<Issue> refactoredIssues = JavaHeuristics.findIssuesWithLines(pr2.getResult().get(), refactoredSource);

                int newScore = ScoreEngine.scoreFromIssues(refactoredIssues);
                newScore = Math.max(newScore, 40);
                result.setRefactoredScore(newScore);
            } else {
                result.setRefactoredScore(0);
            }
        } else {
            // Error handling remains the same
            result.setIssues(List.of(new Issue(0, "Unable to parse Java file. Provide a valid .java file.")));
            result.setOriginalScore(0);
            result.setAiSuggestions(List.of("Parsing failed."));
            result.setRefactoredSource(null);
            result.setRefactoredScore(0);
        }

        return result;
    }

    // Helper method to generate a key for the source code
    private String generateSourceKey(String fileName, String source) {
        // Create a simple hash of filename and source content
        return fileName + "_" + source.hashCode();
    }

    // Update the refactoring method to be more aggressive when needed
    private String applyRefactoringWithAISuggestions(String originalSource, List<String> suggestions,
                                                     String astRefactoredSource, boolean isPreviouslyRefactored) {
        StringBuilder promptBuilder = new StringBuilder();

        if (isPreviouslyRefactored) {
            promptBuilder.append("You are a senior Java expert tasked with PERFECT code refactoring. ");
            promptBuilder.append("This code has been previously refactored but still needs improvement. ");
            promptBuilder.append("Your goal is to achieve 100% quality score by fixing ALL remaining issues.\n\n");
        } else {
            promptBuilder.append("You are a senior Java developer tasked with refactoring code. ");
        }

        promptBuilder.append("Here is the Java code to improve:\n\n```java\n").append(originalSource).append("\n```\n\n");
        promptBuilder.append("Apply these improvements to the code:\n");

        for (String suggestion : suggestions) {
            promptBuilder.append("- ").append(suggestion).append("\n");
        }

        if (isPreviouslyRefactored) {
            promptBuilder.append("\nBe extremely thorough. Fix EVERY issue including those not explicitly mentioned above. ");
            promptBuilder.append("Focus on clean code principles, proper exception handling, removing any hardcoded values, ");
            promptBuilder.append("and ensuring code meets highest quality standards.\n");
        }

        promptBuilder.append("\nProvide ONLY the complete refactored code with no explanations. Begin and end with ```java and ```");

        // Ask OpenAI to refactor the code
        String aiResponse = openAIClient.askModel(promptBuilder.toString());

        // Extract code between ```java and ``` markers
        int startMarker = aiResponse.indexOf("```java");
        int endMarker = aiResponse.lastIndexOf("```");

        if (startMarker != -1 && endMarker != -1 && startMarker < endMarker) {
            String refactoredByAI = aiResponse.substring(startMarker + 7, endMarker).trim();
            if (!refactoredByAI.isEmpty()) {
                return refactoredByAI;
            }
        }

        return astRefactoredSource;
    }

    private List<String> parseAiSuggestions(String aiResponse) {
        List<String> suggestions = new ArrayList<>();

        try {
            // Split response into lines
            String[] lines = aiResponse.split("\n");

            for (String line : lines) {
                line = line.trim();

                // Skip empty lines, headers, code blocks, and markdown
                if (line.isEmpty() ||
                        line.startsWith("#") ||
                        line.startsWith("```") ||
                        line.equals("**Overall Summary:**") ||
                        line.toLowerCase().contains("suggestions for fixing")) {
                    continue;
                }

                // Extract numbered suggestions (1. 2. 3. 4.)
                if (line.matches("^\\d+\\.\\s+\\*\\*.*\\*\\*:.*")) {
                    // Extract the main suggestion text after the colon
                    String[] parts = line.split(":", 2);
                    if (parts.length > 1) {
                        String suggestion = parts[1].trim();
                        // Remove markdown formatting
                        suggestion = suggestion.replaceAll("\\*\\*", "").trim();
                        if (!suggestion.isEmpty() && suggestion.length() > 20) {
                            suggestions.add(suggestion);
                        }
                    }
                }
                // Also capture summary-like suggestions
                else if (line.startsWith("The code contains") ||
                        line.startsWith("Addressing these issues") ||
                        (line.length() > 50 && line.contains("should") &&
                                !line.startsWith("   ") && !line.contains("```"))) {
                    String cleaned = line.replaceAll("\\*\\*", "").trim();
                    if (cleaned.length() > 30) {
                        suggestions.add(cleaned);
                    }
                }
            }

            // If we couldn't extract good suggestions, create them from the content
            if (suggestions.isEmpty() && aiResponse.contains("System.out.println")) {
                suggestions.add("Replace all instances of System.out.println with a logging framework such as SLF4J/Logback");
                suggestions.add("Address TODO/FIXME comments by implementing the necessary changes or removing them");
                suggestions.add("Move hard-coded credentials to a configuration file or use a secrets management tool");
                suggestions.add("Replace broad Exception catching with specific exception handling");
            }

        } catch (Exception e) {
            System.err.println("Error parsing AI suggestions: " + e.getMessage());
            // Fallback suggestions
            suggestions.add("Replace System.out.println with proper logging framework like SLF4J");
            suggestions.add("Address TODO/FIXME comments before production deployment");
            suggestions.add("Move hard-coded credentials to secure configuration");
            suggestions.add("Use specific exception handling instead of broad Exception catches");
        }

        return suggestions;
    }
}
