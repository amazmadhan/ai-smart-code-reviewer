package com.example.test;

public class TestInput {

    public static void main(String[] args) {
        // Example of System.out.println usage
        System.out.println("Hello World!");  

        // TODO comment to simulate a pending task
        // TODO: Implement proper error handling

        // Hard-coded password (to trigger credential detection)
        String password = "mySecret123";

        try {
            int result = 10 / 0; // Will throw exception
        } catch (Exception e) {  // Broad exception
            System.out.println("Error occurred!");
        }

        // Example of a long method (over 50 lines)
        longMethodExample();
    }

    public static void longMethodExample() {
        for (int i = 0; i < 5; i++) System.out.println(i);
        for (int i = 0; i < 5; i++) System.out.println(i);
        for (int i = 0; i < 5; i++) System.out.println(i);
        for (int i = 0; i < 5; i++) System.out.println(i);
        for (int i = 0; i < 5; i++) System.out.println(i);
        for (int i = 0; i < 5; i++) System.out.println(i);
        for (int i = 0; i < 5; i++) System.out.println(i);
        for (int i = 0; i < 5; i++) System.out.println(i);
        for (int i = 0; i < 5; i++) System.out.println(i);
        for (int i = 0; i < 5; i++) System.out.println(i);
        // This makes the method longer than 50 lines
    }
}
