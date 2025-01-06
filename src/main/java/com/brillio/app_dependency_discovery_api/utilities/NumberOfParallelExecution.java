package com.brillio.app_dependency_discovery_api.utilities;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class NumberOfParallelExecution {

    private String name;

    public NumberOfParallelExecution() {
        this.name = "count parallel execution blocks";
    }

    public static int[]  countJobsAndParallelBlocks(String jenkinsfileContent) {
        // Regular expressions to find jobs (stages) and parallel blocks
        Pattern jobPattern = Pattern.compile("stage\\s*\\(\\s*['\"].+?['\"]\\s*\\)", Pattern.CASE_INSENSITIVE);
        Pattern parallelPattern = Pattern.compile("parallel\\s*\\{", Pattern.CASE_INSENSITIVE);

        // Find all matches for jobs (stages)
        Matcher jobMatcher = jobPattern.matcher(jenkinsfileContent);
        int jobCount = 0;
        while (jobMatcher.find()) {
            jobCount++;
        }

        // Find all matches for parallel blocks
        Matcher parallelMatcher = parallelPattern.matcher(jenkinsfileContent);
        int parallelCount = 0;
        while (parallelMatcher.find()) {
            parallelCount++;
        }

        return new int[]{jobCount, parallelCount};
    }

    public String getName() {
        return name;
    }

    public static void main(String[] args) {
        // Sample usage
        String jenkinsfileContent = "..."; // Replace with actual Jenkinsfile content
        NumberOfParallelExecution nope = new NumberOfParallelExecution();
        int[] counts = nope.countJobsAndParallelBlocks(jenkinsfileContent);

        System.out.println("Number of job stages: " + counts[0]);
        System.out.println("Number of parallel blocks: " + counts[1]);
    }
}
