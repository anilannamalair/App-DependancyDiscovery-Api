package com.brillio.app_dependency_discovery_api.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class GitRepoAnalyzer {

    public static void main(String[] args) throws IOException {
        String repoPath = "path_to_local_git_repo"; // Local clone of the repository
        String outputFilePath = "GitRepoAnalysis.json"; // Output JSON file

        List<Map<String, String>> data = analyzeRepo(repoPath);
        writeToJson(data, outputFilePath);

        System.out.println("Analysis completed and written to: " + outputFilePath);
    }

    public static List<Map<String, String>> analyzeRepo(String path) throws IOException {
        List<Map<String, String>> results = new ArrayList<>();

        Files.walk(Paths.get(path)).forEach(filePath -> {
            if (Files.isRegularFile(filePath)) {
                Map<String, String> entry = new HashMap<>();
                String fileName = filePath.getFileName().toString();

                entry.put("File Name", fileName);
                entry.put("File Path", filePath.toString());
                entry.put("Category", categorizeFile(filePath));

                // Artifact Information
                if (fileName.endsWith(".jar") || fileName.endsWith(".zip") || fileName.endsWith(".war")) {
                    entry.put("Artifact Package Type", getFileExtension(fileName));
                    entry.put("Artifact Location", filePath.getParent().toString());
                }

                // Unit Test Details
                if (fileName.contains("test") || fileName.toLowerCase().contains("unittest")) {
                    entry.put("Unit Test", "Present");
                    entry.put("Unit Test Commands", "Sample command"); // Stub for actual command detection
                }

                // Scan Test Details
                if (fileName.contains("scan") || fileName.toLowerCase().contains("sonar")) {
                    entry.put("Scan Test", "Enabled");
                    entry.put("Scan Tools", "SonarQube"); // Example scan tool
                }

                results.add(entry);
            }
        });

        return results;
    }

    private static String categorizeFile(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        
        // Categorize based on specific file extensions
        
        // Artifact files
        if (fileName.endsWith(".jar") || fileName.endsWith(".war") || fileName.endsWith(".zip")) {
            return "Artifact";
        }
        
        // Java related files
        if (fileName.endsWith(".java")) {
            return "Java File";
        }
        
        // Python related files
        if (fileName.endsWith(".py")) {
            return "Python File";
        }

        // .NET related files
        if (fileName.endsWith(".cs")) {
            return "C# File (DotNet)";
        }
        
        // JavaScript and TypeScript files
        if (fileName.endsWith(".js")) {
            return "JavaScript File";
        } else if (fileName.endsWith(".ts")) {
            return "TypeScript File";
        }
        
        // React.js and Angular related files (common extensions like JSX and TSX)
        if (fileName.endsWith(".jsx")) {
            return "React.js JSX File";
        } else if (fileName.endsWith(".tsx")) {
            return "React.js TSX File";
        }
        
        // HTML/CSS/JS
        if (fileName.endsWith(".html")) {
            return "HTML File";
        } else if (fileName.endsWith(".css")) {
            return "CSS File";
        }
        
        // Configuration files
        if (fileName.endsWith(".json") || fileName.endsWith(".yaml") || fileName.endsWith(".yml") || fileName.endsWith(".xml")) {
            return "Configuration File";
        } else if (fileName.endsWith(".properties")) {
            return "Properties File";
        } else if (fileName.endsWith(".gitignore")) {
            return "Git Ignore File";
        }
        
        // Shell script and batch files
        if (fileName.endsWith(".sh")) {
            return "Shell Script";
        } else if (fileName.endsWith(".bat")) {
            return "Batch Script";
        }
        
        // Markdown files
        if (fileName.endsWith(".md")) {
            return "Markdown File";
        }
        
        // Project files
        if (fileName.endsWith(".pom")) {
            return "Maven Project File";
        } else if (fileName.endsWith(".gradle")) {
            return "Gradle Build File";
        } else if (fileName.endsWith(".csproj")) {
            return "C# Project File (DotNet)";
        } else if (fileName.endsWith(".sln")) {
            return "Solution File (DotNet)";
        }
        
        // Miscellaneous text and document files
        if (fileName.endsWith(".txt")) {
            return "Text File";
        }
        
        // Other common extensions
        if (fileName.endsWith(".jsp")) {
            return "JavaServer Pages (JSP)";
        } else if (fileName.endsWith(".asp")) {
            return "Active Server Pages (ASP)";
        }
        
        // Unknown category for files with no matched extensions
        return "Unknown File Type";
    }



    private static String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private static void writeToJson(List<Map<String, String>> data, String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Write the list of maps as a JSON file
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(fileWriter, data);
        }
    }
}
