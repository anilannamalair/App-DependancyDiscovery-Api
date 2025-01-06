package com.brillio.app_dependency_discovery_api.utilities;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberOfLibraries {

    private String name;

    public NumberOfLibraries() {
        this.name = "number of libraries";
    }

    public static List<String> getLibrariesFromJenkinsfileContent(String jenkinsfileContent) {
        List<String> libraries = new ArrayList<>();
        // Compile a regular expression pattern to match lines starting with 'library' followed by a string in double quotes
        Pattern libraryPattern = Pattern.compile("^\\s*library\\s+\"([^\"]+)\"");

        // Split the content into lines and iterate over each line
        String[] lines = jenkinsfileContent.split("\\R");
        for (String line : lines) {
            Matcher match = libraryPattern.matcher(line);
            if (match.find()) {
                // If a match is found, extract the library name and add it to the list
                libraries.add(match.group(1));
            }
        }

        // Return the list of libraries found in the Jenkinsfile content
        return libraries;
    }

    public String getName() {
        return name;
    }

    public static void main(String[] args) {
        // Sample usage
        String jenkinsfileContent = "..."; // Replace with actual Jenkinsfile content
        NumberOfLibraries nol = new NumberOfLibraries();
        List<String> libraries = nol.getLibrariesFromJenkinsfileContent(jenkinsfileContent);

        System.out.println("Libraries found: " + libraries);
    }
}
