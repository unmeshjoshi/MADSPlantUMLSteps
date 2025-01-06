package com.pumlsteps;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StepParser {
    private static final Pattern METADATA_PATTERN = Pattern.compile("'\\s*\\[step(\\d+)\\s*\\{(.*)\\}\\]");
    private static final Pattern SIMPLE_STEP_PATTERN = Pattern.compile("'\\s*\\[step(\\d+)\\]");

    private static final Pattern STEP_START_PATTERN = Pattern.compile("'\\s*\\[step(\\d+)(?:\\s*\\{(.*)\\})?\\]");
    private static final Pattern STEP_END_PATTERN = Pattern.compile("'\\s*\\[/step(\\d+)]");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isStepStart(String line) {
        return STEP_START_PATTERN.matcher(line.trim()).matches();
    }

    public boolean isStepEnd(String line, int stepNumber) {
        Matcher matcher = STEP_END_PATTERN.matcher(line.trim());
        return matcher.matches() && Integer.parseInt(matcher.group(1)) == stepNumber;
    }

    public Map<String, Object> parseMetadata(String line) {
        Matcher matcher = METADATA_PATTERN.matcher(line.trim());
        if (matcher.find()) {
            try {
                String metadataJson = matcher.group(2);
                Map<String, Object> metadata = metadataJson == null || metadataJson.isEmpty()
                        ? new HashMap<>()
                        : objectMapper.readValue("{" + metadataJson + "}", Map.class);
                metadata.put("step", Integer.parseInt(matcher.group(1))); // Add step number
                return metadata;
            } catch (Exception e) {
                throw new RuntimeException("Error parsing metadata: " + e.getMessage(), e);
            }
        }
        // Handle cases like '[step18]' without metadata
        Matcher simpleStepMatcher = SIMPLE_STEP_PATTERN.matcher(line.trim());
        if (simpleStepMatcher.matches()) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("step", Integer.parseInt(simpleStepMatcher.group(1)));
            return metadata;
        }
        return Collections.emptyMap();
    }

    public ParsedPlantUmlFile parse(PumlFile sourceFile) throws IOException {
        String fileName = sourceFile.getBaseName();
        List<Step> steps = new ArrayList<>();
        List<String> lines = sourceFile.readLines();
        StringBuilder currentContent = new StringBuilder();
        List<String> includes = new ArrayList<>();
        List<String> participants = new ArrayList<>();

        boolean inStep = false;
        int currentStep = -1;

        for (String line : lines) {
            if (line.trim().startsWith("!include")) {
                addInclude(line, includes, currentContent);

            } else if (line.trim().startsWith("participant")) {
                participants.add(line);
                currentContent.append(line).append("\n");

            } else if (isStepStart(line)) {
                if (inStep) {
                    throw new IllegalStateException("Nested steps are not allowed.");
                }
                inStep = true;
                currentStep = createStep(line, currentContent, includes, participants, steps);

            } else if (isStepEnd(line, currentStep)) {
                inStep = false;
                currentStep = endStep(steps, currentContent);

            } else if (inStep) {
                currentContent.append(line).append("\n");
            } else {
                currentContent.append(line).append("\n"); // Append lines outside step tags
            }
        }

        // Handle case where step is not properly closed
        if (inStep) {
            Step step = steps.get(steps.size() - 1);
            step.setContent(currentContent.toString());
        }

        // Handle case where no start or end tags are present
        if (steps.isEmpty() && currentContent.length() > 0) {
            Map<String, Object> defaultMetadata = Map.of(
                    "step", 1,
                    "name", "Default Step",
                    "author", "Unknown"
            );
            Step defaultStep = new Step(1, defaultMetadata);
            defaultStep.setContent(currentContent.toString());
            steps.add(defaultStep);
        }

        return new ParsedPlantUmlFile(fileName, steps);
    }

    private static int endStep(List<Step> steps, StringBuilder currentContent) {
        int currentStep;
        Step step = steps.get(steps.size() - 1); // Get the last added step
        step.setContent(currentContent.toString() + "\n@enduml");
        currentStep = -1;
        return currentStep;
    }

    private int createStep(String line, StringBuilder currentContent, List<String> includes, List<String> participants, List<Step> steps) {
        int currentStep;
        Map<String, Object> metadata = parseMetadata(line);
        currentStep = (int) metadata.get("step");
        if (metadata.containsKey("newPage")) {
            currentContent.setLength(0);
            currentContent.append("@startuml\n");
            addIncludes(currentContent, includes);
            addParticipants(currentContent, participants);
        }
        steps.add(new Step(currentStep, metadata));
        return currentStep;
    }

    private void addParticipants(StringBuilder currentContent, List<String> participants) {
        for (String participant : participants) {
            currentContent.append(participant).append("\n");
        }
    }

    private static void addIncludes(StringBuilder currentContent, List<String> includes) {
        for (String include : includes) {
            currentContent.append(include).append("\n");
        }
    }

    private static void addInclude(String line, List<String> includes, StringBuilder currentContent) {
        // Store includes for reuse
        includes.add(line);
        currentContent.append(line).append("\n");
    }

}
