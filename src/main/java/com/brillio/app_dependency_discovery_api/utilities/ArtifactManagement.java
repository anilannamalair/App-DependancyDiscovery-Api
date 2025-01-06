package com.brillio.app_dependency_discovery_api.utilities;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

public class ArtifactManagement {

    private String name;

    public ArtifactManagement() {
        this.name = "find artifact";
    }

    public static class ArtifactoryResult {
        public boolean isPresent;
        public List<String> artifactoryLines;

        public ArtifactoryResult(boolean isPresent, List<String> artifactoryLines) {
            this.isPresent = isPresent;
            this.artifactoryLines = artifactoryLines;
        }
    }

    public static ArtifactoryResult checkArtifactoryInJenkinsfile(String jenkinsfileContent) {
        // Regular expression to find the environment section
        Pattern envPattern = Pattern.compile("environment\\s*\\{[^}]*\\}", Pattern.MULTILINE);
        Matcher envMatcher = envPattern.matcher(jenkinsfileContent);

        if (envMatcher.find()) {
            String envContent = envMatcher.group(0);

            // Check for Artifactory server and credentials
            boolean serverPresent = envContent.contains("artifactory_server");
            boolean credPresent = envContent.contains("artifactory_cred");

            if (serverPresent && credPresent) {
                // Extract the lines containing the Artifactory configuration
                Pattern artifactoryPattern = Pattern.compile("(artifactory_server\\s*=\\s*.*|artifactory_cred\\s*=\\s*.*)");
                Matcher artifactoryMatcher = artifactoryPattern.matcher(envContent);
                List<String> artifactoryLines = new ArrayList<>();

                while (artifactoryMatcher.find()) {
                    artifactoryLines.add(artifactoryMatcher.group(0));
                }

                return new ArtifactoryResult(true, artifactoryLines);
            } else {
                return new ArtifactoryResult(false, new ArrayList<>());
            }
        } else {
            return new ArtifactoryResult(false, new ArrayList<>());
        }
    }

    public String getName() {
        return name;
    }

    public static void main(String[] args) {
        // Sample usage
        String jenkinsfileContent = "..."; // Replace with actual Jenkinsfile content
        ArtifactManagement artifactManagement = new ArtifactManagement();
        ArtifactoryResult result = artifactManagement.checkArtifactoryInJenkinsfile(jenkinsfileContent);

        if (result.isPresent) {
            System.out.println("Artifactory configuration is present in the Jenkinsfile.");
            System.out.println("Artifactory lines:");
            for (String line : result.artifactoryLines) {
                System.out.println(line);
            }
        } else {
            System.out.println("Artifactory configuration is NOT present in the Jenkinsfile.");
        }
    }
}