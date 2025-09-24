package com.ai.hackathon.model;

public class Issue {
    private int line;
    private String message;

    public Issue() {}

    public Issue(int line, String message) { this.line = line; this.message = message; }

    public int getLine() { return line; }
    public void setLine(int line) { this.line = line; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
