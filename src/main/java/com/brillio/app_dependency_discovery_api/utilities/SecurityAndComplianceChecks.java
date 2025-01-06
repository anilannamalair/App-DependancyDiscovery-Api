package com.brillio.app_dependency_discovery_api.utilities;
import java.util.ArrayList;
import java.util.List;

public class SecurityAndComplianceChecks {

    private String name;

    public SecurityAndComplianceChecks() {
        this.name = "get security tools";
    }

    public static List<String> findSecurityToolsInJenkinsfile(String jenkinsfileContent, List<String> securityToolsList) {
        List<String> foundTools = new ArrayList<>();
        for (String tool : securityToolsList) {
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
        List<String> securityToolsList = List.of("Tool1", "Tool2", "Tool3"); // Replace with actual security tools

        SecurityAndComplianceChecks scc = new SecurityAndComplianceChecks();
        List<String> foundTools = scc.findSecurityToolsInJenkinsfile(jenkinsfileContent, securityToolsList);

        System.out.println("Found security tools: " + foundTools);
    }
}
