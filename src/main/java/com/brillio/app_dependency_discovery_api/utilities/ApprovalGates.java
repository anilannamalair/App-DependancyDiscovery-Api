package com.brillio.app_dependency_discovery_api.utilities;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApprovalGates {

    private String name;

    public ApprovalGates() {
        this.name = "count approval stages";
    }

    public static int countApprovalStages(String jenkinsfileContent) {
        // Regular expression to find 'Approval' stage
        Pattern approvalPattern = Pattern.compile("stage\\s*\\(\\s*['\"]Approval['\"]\\s*\\)", Pattern.CASE_INSENSITIVE);

        // Find all matches
        Matcher matcher = approvalPattern.matcher(jenkinsfileContent);

        // Count the number of matches
        int approvalCount = 0;
        while (matcher.find()) {
            approvalCount++;
        }

        return approvalCount;
    }

    public String getName() {
        return name;
    }

    public static void main(String[] args) {
        // Sample usage
        String jenkinsfileContent = "..."; // Replace with actual Jenkinsfile content
        ApprovalGates approvalGates = new ApprovalGates();
        int approvalCount = approvalGates.countApprovalStages(jenkinsfileContent);
        System.out.println("Number of approval stages: " + approvalCount);
    }
}