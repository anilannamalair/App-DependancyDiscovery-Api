package com.brillio.app_dependency_discovery_api.utilities;

import java.util.ArrayList;
import java.util.List;

public class TestingAndCodeQualityChecks {

    private String name;

    public TestingAndCodeQualityChecks() {
        this.name = "get tools for testing";
    }

    public static List<String> findToolsInJenkinsfile(String jenkinsfileContent, List<String> testingToolsList) {
        List<String> foundTools = new ArrayList<>();
        for (String tool : testingToolsList) {
            if (jenkinsfileContent.contains(tool)) {
                foundTools.add(tool);
            }
        }
        return foundTools;
    }

    public String getName() {
        return name;
    }

    public static void main(String[] args) {
        // Sample usage
        String jenkinsfileContent = "..."; // Replace with actual Jenkinsfile content
        List<String> testingToolsList = List.of("Tool1", "Tool2", "Tool3"); // Replace with actual testing tools

        TestingAndCodeQualityChecks tcqc = new TestingAndCodeQualityChecks();
        List<String> foundTools = tcqc.findToolsInJenkinsfile(jenkinsfileContent, testingToolsList);

        System.out.println("Found testing tools: " + foundTools);
    }
}