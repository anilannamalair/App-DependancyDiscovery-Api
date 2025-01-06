package com.brillio.app_dependency_discovery_api.utilities;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class NumberOfEnvironmentsOrDeployments {

    private String name;

    public NumberOfEnvironmentsOrDeployments() {
        this.name = "get deploy stages";
    }

    public static int countDeployStages(String jenkinsfileContent) {
        // Regex to match stages with 'deploy' in their names
        Pattern deployStagePattern = Pattern.compile("stage\\s*\\(\\s*['\"]deploy['\"]\\s*\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = deployStagePattern.matcher(jenkinsfileContent);

        int deployStagesCount = 0;
        while (matcher.find()) {
            deployStagesCount++;
        }
        return deployStagesCount;
    }

    public String getName() {
        return name;
    }

    public static void main(String[] args) {
        // Sample usage
        String jenkinsfileContent = "..."; // Replace with actual Jenkinsfile content
        NumberOfEnvironmentsOrDeployments noed = new NumberOfEnvironmentsOrDeployments();
        int deployStagesCount = noed.countDeployStages(jenkinsfileContent);
        System.out.println("Number of deploy stages: " + deployStagesCount);
    }
}