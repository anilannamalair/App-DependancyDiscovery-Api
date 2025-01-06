package com.brillio.app_dependency_discovery_api.utilities;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ErrorHandlingAndRetryLogic {

    private String name;

    public ErrorHandlingAndRetryLogic() {
        this.name = "find artifact";
    }

    public static int[] countTryAndRetryBlocks(String jenkinsfileContent) {
        // Regular expressions to find try and retry blocks
        Pattern tryPattern = Pattern.compile("\\btry\\b", Pattern.MULTILINE);
        Pattern retryPattern = Pattern.compile("\\bretry\\s*\\(\\d+\\)", Pattern.MULTILINE);

        // Find all matches for try blocks
        Matcher tryMatcher = tryPattern.matcher(jenkinsfileContent);
        int tryCount = 0;
        while (tryMatcher.find()) {
            tryCount++;
        }

        // Find all matches for retry blocks
        Matcher retryMatcher = retryPattern.matcher(jenkinsfileContent);
        int retryCount = 0;
        while (retryMatcher.find()) {
            retryCount++;
        }

        return new int[]{tryCount, retryCount};  // Return counts as an array
    }


    public String getName() {
        return name;
    }

    public static void main(String[] args) {
        // Sample usage
        String jenkinsfileContent = "..."; // Replace with actual Jenkinsfile content
        ErrorHandlingAndRetryLogic logic = new ErrorHandlingAndRetryLogic();
        int[] counts = logic.countTryAndRetryBlocks(jenkinsfileContent);
        System.out.println("Number of try blocks: " + counts[0]);
        System.out.println("Number of retry blocks: " + counts[1]);
    }
}