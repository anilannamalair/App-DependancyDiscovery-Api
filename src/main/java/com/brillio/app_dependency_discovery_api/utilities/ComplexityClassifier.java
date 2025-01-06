package com.brillio.app_dependency_discovery_api.utilities;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ComplexityClassifier {

    private String name;

    public ComplexityClassifier() {
        this.name = "classify complexity";
    }

    public static String classifyJenkinsfile(String jenkinsfileContent) {
        // Regular expression to match any library declaration
        Pattern libraryPattern = Pattern.compile("^\\s*library\\s+\"[^\"]+\"", Pattern.MULTILINE);

        // Check if the Jenkinsfile is 'Complex'
        Matcher libraryMatcher = libraryPattern.matcher(jenkinsfileContent);
        if (libraryMatcher.find()) {
            return "Complex";
        }

        // Check if the Jenkinsfile is 'Medium'
        if (jenkinsfileContent.contains("node {") || jenkinsfileContent.contains("sh") || jenkinsfileContent.contains("bat")) {
            return "Medium";
        }

        // Check if the Jenkinsfile is 'Simple'
        if (jenkinsfileContent.contains("pipeline {") && !jenkinsfileContent.contains("sh") && !jenkinsfileContent.contains("bat")) {
            return "Simple";
        }

        // If none of the above conditions are met, classify as 'Unknown'
        return "Unknown";
    }

    public String getName() {
        return name;
    }

    public static void main(String[] args) {
        // Sample usage
        String jenkinsfileContent = "..."; // Replace with actual Jenkinsfile content
        ComplexityClassifier classifier = new ComplexityClassifier();
        String classification = classifier.classifyJenkinsfile(jenkinsfileContent);
        System.out.println("Jenkinsfile classification: " + classification);
    }
}