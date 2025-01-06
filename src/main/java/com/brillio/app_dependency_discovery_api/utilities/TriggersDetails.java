package com.brillio.app_dependency_discovery_api.utilities;
import java.util.regex.*;
import java.util.ArrayList;
import java.util.List;

public class TriggersDetails {

    private String name;

    public TriggersDetails() {
        this.name = "triggers count";
    }

    public static class TriggerResult {
        private int count;
        private List<String> triggers;

        public TriggerResult(int count, List<String> triggers) {
            this.count = count;
            this.triggers = triggers;
        }

        public int getCount() {
            return count;
        }

        public List<String> getTriggers() {
            return triggers;
        }
    }

    public static TriggerResult extractTriggers(String content) {
        // Remove single line comments
        content = content.replaceAll("//.*", "");

        // Remove multi-line comments
        content = content.replaceAll("/\\*.*?\\*/", "");

        // Find the triggers section
        Pattern pattern = Pattern.compile("triggers\\s*\\{([^}]*)\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return new TriggerResult(0, new ArrayList<>());
        }

        String triggersContent = matcher.group(1);

        // Extract triggers
        List<String> triggers = new ArrayList<>();
        Pattern triggerPattern = Pattern.compile("\\b\\w+\\s*\\(.*?\\)|\\b\\w+\\s*\".*?\"");
        matcher = triggerPattern.matcher(triggersContent);

        while (matcher.find()) {
            triggers.add(matcher.group());
        }

        return new TriggerResult(triggers.size(), triggers);
    }

    public static void main(String[] args) {
        // Example content
        String content = "triggers { eventA() { /* comment */ } eventB(\"test\"); // single line comment }";

        TriggerResult result = extractTriggers(content);
        System.out.println("Trigger count: " + result.getCount());
        System.out.println("Triggers: " + result.getTriggers());
    }

}