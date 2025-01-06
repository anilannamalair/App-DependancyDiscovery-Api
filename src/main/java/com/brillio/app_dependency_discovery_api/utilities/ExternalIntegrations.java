package com.brillio.app_dependency_discovery_api.utilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ExternalIntegrations {

    private String name;

    public ExternalIntegrations() {
        this.name = "get external integrations";
    }

    public static List<String> extractToolsSectionFirstWords(String jenkinsfilePath) {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(jenkinsfilePath));
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>(); // Return empty list if there's an error
        }

        boolean toolsSection = false;
        List<String> firstWords = new ArrayList<>(); // Initialize as an empty list

        for (String line : lines) {
            String strippedLine = line.trim();
            if (strippedLine.startsWith("tools {")) {
                toolsSection = true;
                continue;
            }
            if (toolsSection) {
                if (strippedLine.equals("}")) {
                    break;
                }
                String[] words = strippedLine.split("\\s+");
                if (words.length > 0) {
                    firstWords.add(words[0]);
                }
            }
        }

        return firstWords; // Always return a list, never null
   }


    public String getName() {
        return name;
    }

    public static void main(String[] args) {
        // Sample usage
        String jenkinsfilePath = "path/to/Jenkinsfile"; // Replace with actual Jenkinsfile path
        ExternalIntegrations ei = new ExternalIntegrations();
        List<String> firstWords = ei.extractToolsSectionFirstWords(jenkinsfilePath);

        if (firstWords != null) {
            System.out.println("First words in the 'tools' section:");
            for (String word : firstWords) {
                System.out.println(word);
            }
        } else {
            System.out.println("No 'tools' section found in the Jenkinsfile.");
        }
    }
}