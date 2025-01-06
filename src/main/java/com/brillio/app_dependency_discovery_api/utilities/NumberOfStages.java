package com.brillio.app_dependency_discovery_api.utilities;
public class NumberOfStages {

    private String name;

    public NumberOfStages() {
        this.name = "number of stages";
    }

    public static int countStagesInJenkinsfile(String jenkinsfileContent) {
        // Count the number of stages
        int stagesCount = 0;
        int index = 0;
        while ((index = jenkinsfileContent.indexOf("stage(", index)) != -1) {
            stagesCount++;
            index += "stage(".length();
        }
        return stagesCount;
    }

    public String getName() {
        return name;
    }

    public static void main(String[] args) {
        // Sample usage
        String jenkinsfileContent = "..."; // Replace with actual Jenkinsfile content
        NumberOfStages nos = new NumberOfStages();
        int stagesCount = nos.countStagesInJenkinsfile(jenkinsfileContent);
        System.out.println("Number of stages: " + stagesCount);
    }
}