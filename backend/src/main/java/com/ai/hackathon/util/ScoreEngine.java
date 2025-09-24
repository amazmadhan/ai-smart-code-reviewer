package com.ai.hackathon.util;

import com.ai.hackathon.model.Issue;
import java.util.List;

public class ScoreEngine {

    // Calculates score based on issues found
    public static int scoreFromIssues(List<Issue> issues) {
        if (meetsAllStandards(issues)) {
            return 100; // Perfect score if no issues
        }
        int base = 100;
        int penalty = JavaHeuristics.computePenaltyFromIssues(issues);
        return Math.max(0, base - penalty); // Deduct penalty for issues
    }

    // Checks if all standards are met (no issues)
    private static boolean meetsAllStandards(List<Issue> issues) {
        return issues == null || issues.isEmpty();
    }
}
