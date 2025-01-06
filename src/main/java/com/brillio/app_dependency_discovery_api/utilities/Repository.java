package com.brillio.app_dependency_discovery_api.utilities;
import java.util.ArrayList; 
import java.util.List;
import java.util.Map;

public class Repository {

    private String name;

    public Repository() {
        this.name = "get version_control repository tools";
    }

    public static List<String> findRepositoryToolsInJenkinsfile(String jenkinsfileContent, Map<String, String> repositoryToolsDict) {
        List<String> foundTools = new ArrayList<>();
        for (Map.Entry<String, String> entry : repositoryToolsDict.entrySet()) {
            if (jenkinsfileContent.contains(entry.getKey())) {
                foundTools.add(entry.getValue());
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
        Map<String, String> repositoryToolsDict = Map.of("git", "Git", "svn", "Subversion");
        
        Repository repo = new Repository();
        List<String> tools = repo.findRepositoryToolsInJenkinsfile(jenkinsfileContent, repositoryToolsDict);
        System.out.println("Found repository tools: " + tools);
    }
}