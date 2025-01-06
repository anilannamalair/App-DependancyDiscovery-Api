package com.brillio.app_dependency_discovery_api.utilities;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariablesFinder {

    private String name;

    public VariablesFinder() {
        this.name = "fetch all variables in the repo";
    }

    public String getName() {
        return name;
    }

    public static List<String[]> findLinesWithDollarSignEnvironmentVariables(String folderPath) {
        System.out.println("---------------------"+folderPath);
    	List<String[]> linesWithDollar = new ArrayList<>();

        try {
            Files.walk(Paths.get(folderPath)).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.contains("${")) {
                                linesWithDollar.add(new String[] { filePath.toString(), line.trim() });
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading " + filePath + ": " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            System.err.println("Error walking through " + folderPath + ": " + e.getMessage());
        }

        return linesWithDollar;
    }

    public static int writeOutputToFile(String folderPath, List<String[]> linesWithDollar) {
        String outputFilePath = folderPath + File.separator + "Environment_variables_lines.txt";

        try {
            Files.deleteIfExists(Paths.get(outputFilePath));
            try (FileWriter writer = new FileWriter(outputFilePath)) {
                for (String[] fileLine : linesWithDollar) {
                    writer.write("File: " + fileLine[0] + " -> Line: " + fileLine[1] + "\n");
                }
            }
            System.out.println("Output written to " + outputFilePath);
            return 200;
        } catch (IOException e) {
            System.err.println("Error writing to " + outputFilePath + ": " + e.getMessage());
            return 500;
        }
    }

    public static Map<String, Integer> findValidVariables(String folderPath) {
        Pattern variablePattern = Pattern.compile("\\$\\{([A-Za-z0-9_]+)\\}");
        Pattern invalidPattern = Pattern.compile("\\$\\{.*\\$@.*\\}");
        Map<String, Integer> variables = new HashMap<>();

        try {
            Files.walk(Paths.get(folderPath)).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            Matcher matcher = variablePattern.matcher(line);
                            while (matcher.find()) {
                                String variable = "${" + matcher.group(1) + "}";
                                if (!invalidPattern.matcher(variable).find()) {
                                    variables.put(variable, variables.getOrDefault(variable, 0) + 1);
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading " + filePath + ": " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            System.err.println("Error walking through " + folderPath + ": " + e.getMessage());
        }

        return variables;
    }

    public static void main(String[] args) {
        // Sample usage
        String folderPath = "..."; // Replace with actual folder path
        VariablesFinder finder = new VariablesFinder();

        // Find lines with dollar sign environment variables
        List<String[]> linesWithDollar = finder.findLinesWithDollarSignEnvironmentVariables(folderPath);
        System.out.println(linesWithDollar);

        // Write output to file
        finder.writeOutputToFile(folderPath, linesWithDollar);

        // Find valid variables and their count
        Map<String, Integer> validVariables = finder.findValidVariables(folderPath);
        System.out.println(validVariables);

        // Get the list of variables
        List<String> variablesList = new ArrayList<>(validVariables.keySet());
        System.out.println("Variables List: " + variablesList);

        // Get the total count of all variables
        int totalCount = validVariables.values().stream().mapToInt(Integer::intValue).sum();
        System.out.println("Total Count of Variables: " + totalCount);
    }
}