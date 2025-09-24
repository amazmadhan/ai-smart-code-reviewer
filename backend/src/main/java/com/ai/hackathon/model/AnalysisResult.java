package com.ai.hackathon.model;

import java.util.List;

public class AnalysisResult {
    private String fileName;
    private String originalSource;
    private int originalScore;
    private List<Issue> issues;
    private List<String> aiSuggestions;
    private String refactoredSource;
    private int refactoredScore;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalSource() {
        return originalSource;
    }

    public void setOriginalSource(String originalSource) {
        this.originalSource = originalSource;
    }

    public int getOriginalScore() {
        return originalScore;
    }

    public void setOriginalScore(int originalScore) {
        this.originalScore = originalScore;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public void setIssues(List<Issue> issues) {
        this.issues = issues;
    }

    public List<String> getAiSuggestions() {
        return aiSuggestions;
    }

    public void setAiSuggestions(List<String> aiSuggestions) {
        this.aiSuggestions = aiSuggestions;
    }

    public String getRefactoredSource() {
        return refactoredSource;
    }

    public void setRefactoredSource(String refactoredSource) {
        this.refactoredSource = refactoredSource;
    }

    public int getRefactoredScore() {
        return refactoredScore;
    }

    public void setRefactoredScore(int refactoredScore) {
        this.refactoredScore = refactoredScore;
    }
}
