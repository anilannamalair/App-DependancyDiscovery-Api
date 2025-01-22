package com.brillio.app_dependency_discovery_api.utilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonToFileStructure {

    // Method that accepts Map<String, Object> and returns list of file paths
    public List<String> convertMapToFilePaths(Map<String, Object> repoStructure) {
        List<String> fileStructure = new ArrayList<>();
        
        // Convert the Map structure to a list of file paths
        generateFilePaths(repoStructure, "", fileStructure);
        
        return fileStructure;
    }

    // Recursive function to generate file paths from Map structure
    private void generateFilePaths(Map<String, Object> map, String currentPath, List<String> fileStructure) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String newPath = currentPath.isEmpty() ? key : currentPath + "/" + key;
            
            if (value instanceof Map) {
                // If value is a Map, recurse into it (i.e., it's a folder)
                generateFilePaths((Map<String, Object>) value, newPath, fileStructure);
            } else if (value instanceof String && value.equals("file")) {
                // If value is a file, add the current path to the list
                fileStructure.add(newPath);
            }
        }
    }
}
