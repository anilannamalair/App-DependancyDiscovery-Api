package com.brillio.app_dependency_discovery_api.utilities;
import java.util.ArrayList;
import java.util.List;

public class CloudInfraAndContainerization {

    private String name;

    public CloudInfraAndContainerization() {
        this.name = "get containerization tools";
    }

    public static List<String> findContainerizationToolsInJenkinsfile(String jenkinsfileContent, List<String> containerizationToolsList) {
        List<String> foundTools = new ArrayList<>();
        for (String tool : containerizationToolsList) {
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
        List<String> containerizationToolsList = List.of("docker", "kubectl", "openshift"); // Sample tools list
        CloudInfraAndContainerization ciac = new CloudInfraAndContainerization();
        List<String> foundTools = ciac.findContainerizationToolsInJenkinsfile(jenkinsfileContent, containerizationToolsList);

        System.out.println("Found containerization tools: " + foundTools);
    }
}