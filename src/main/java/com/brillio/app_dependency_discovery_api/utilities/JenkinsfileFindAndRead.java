package com.brillio.app_dependency_discovery_api.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JenkinsfileFindAndRead {

    private String name;

    public JenkinsfileFindAndRead() {
        this.name = "assessment per repo";
    }

    public String getName() {
        return name;
    }

    public static String findJenkinsfile(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    String found = findJenkinsfile(file.getAbsolutePath());
                    if (found != null) {
                        return found;
                    }
                } else if (file.getName().equals("Jenkinsfile")) {
                    return file.getAbsolutePath();
                }
            }
        }
        return null;
    }
    
    public static Triple<String, String, Integer> findReadJenkinsfile(String jenkinsFolderPath) {
        String jenkinsfilePath = findJenkinsfile(jenkinsFolderPath);

        if (jenkinsfilePath == null) {
            System.out.println("Jenkinsfile not found");
            return new Triple<>("Jenkinsfile not found", null, 500);
        }

        try {
            String jenkinsfileContent = new String(Files.readAllBytes(Paths.get(jenkinsfilePath)));

            // Remove single-line comments
            jenkinsfileContent = jenkinsfileContent.replaceAll("//.*", "");

            // Remove multi-line comments, including those with asterisks at the beginning of each line
            Pattern pattern = Pattern.compile("/\\*[\\s\\S]*?\\*/");
            Matcher matcher = pattern.matcher(jenkinsfileContent);
            jenkinsfileContent = matcher.replaceAll("");

            return new Triple<>(jenkinsfileContent, jenkinsfilePath, 200);
        } catch (IOException e) {
            System.err.println("Error reading Jenkinsfile: " + e.getMessage());
            return new Triple<>("Error reading Jenkinsfile", null, 500);
        }
    }

    public static void main(String[] args) {
        // Sample usage
        JenkinsfileFindAndRead jenkinsfileFinder = new JenkinsfileFindAndRead();
        String jenkinsFolderPath = "path/to/jenkins/folder"; // Replace with actual folder path

        Triple<String, String, Integer> result = findReadJenkinsfile(jenkinsFolderPath);
        System.out.println("Content: " + result.getFirst());
        System.out.println("Path: " + result.getSecond());
        System.out.println("Status Code: " + result.getThird());
    }
}
//
//class Triple<F, S, T> {
//    private final F first;
//    private final S second;
//    private final T third;
//
//    public Triple(F first, S second, T third) {
//        this.first = first;
//        this.second = second;
//        this.third = third;
//    }
//
//    public F getFirst() {
//        return first;
//    }
//
//    public S getSecond() {
//        return second;
//    }
//
//    public T getThird() {
//        return third;
//    }
//}
