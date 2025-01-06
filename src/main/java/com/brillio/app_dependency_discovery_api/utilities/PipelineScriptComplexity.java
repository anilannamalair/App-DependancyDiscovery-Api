package com.brillio.app_dependency_discovery_api.utilities;
public class PipelineScriptComplexity {

    private String name;

    public PipelineScriptComplexity() {
        this.name = "identify the pipeline type";
    }

    public static String identifyPipelineType(String jenkinsfileContent) {
        if (jenkinsfileContent.contains("pipeline {") && (jenkinsfileContent.contains("sh") || jenkinsfileContent.contains("bat"))) {
            return "Partially Scripted";
        } else if (jenkinsfileContent.contains("pipeline {") && !jenkinsfileContent.contains("sh") && !jenkinsfileContent.contains("bat")) {
            return "Declarative Pipeline";
        } else if (jenkinsfileContent.contains("node {")) {
            return "Scripted Pipeline";
        } else {
            return "Unknown Pipeline Type";
        }
    }

    public String getName() {
        return name;
    }

    public static void main(String[] args) {
        // Sample usage
        String jenkinsfileContent = "..."; // Replace with actual Jenkinsfile content
        PipelineScriptComplexity psc = new PipelineScriptComplexity();
        String pipelineType = psc.identifyPipelineType(jenkinsfileContent);
        System.out.println("Pipeline type: " + pipelineType);
    }
}