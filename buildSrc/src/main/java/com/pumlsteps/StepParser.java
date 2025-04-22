package com.pumlsteps;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StepParser {
    private static final Pattern STEP_START_PATTERN = Pattern.compile("'\\s*\\[step(?:\\d+)?(?:\\s*\\{(.*)\\})?\\]");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isStepStart(String line) {
        return STEP_START_PATTERN.matcher(line.trim()).matches();
    }
    
    public Map<String, Object> parseMetadata(String line) {
        Matcher matcher = STEP_START_PATTERN.matcher(line.trim());
        if (matcher.find()) {
            try {
                String metadataJson = matcher.group(1);
                if (metadataJson == null || metadataJson.isEmpty()) {
                    return new HashMap<>();
                }
                return objectMapper.readValue("{" + metadataJson + "}", Map.class);
            } catch (Exception e) {
                throw new RuntimeException("Error parsing metadata: " + e.getMessage(), e);
            }
        }
        return Collections.emptyMap();
    }

    public ParsedPlantUmlFile parse(PumlFile sourceFile) throws IOException {
        List<Step> steps = new ArrayList<>();


        List<String> includes = new ArrayList<>();
        List<String> participants = new ArrayList<>();

        List<String> lines = sourceFile.readLines();
        StringBuilder currentContent = new StringBuilder();
        for (String line : lines) {
            if (line.trim().startsWith("!include")) {
                addInclude(line, includes, currentContent);

            } else if (line.trim().startsWith("participant")) {
                participants.add(line);
                currentContent.append(line).append("\n");

            } else if (isStepStart(line)) {
                endPreviousStep(steps, currentContent);
                createStep(line, currentContent, includes, participants, steps);

            } else {
                currentContent.append(line).append("\n"); // Append lines outside step tags
            }
        }

        //last step content.
        if (!steps.isEmpty()) {
          endPreviousStep(steps, currentContent);
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

        return new ParsedPlantUmlFile(sourceFile.getBaseName(), steps);
    }

    private static void endPreviousStep(List<Step> steps, StringBuilder currentContent) {
        if (steps.isEmpty()) {
            return;
        }
        Step step = steps.get(steps.size() - 1); // Get the last added step
        step.setContent(currentContent.toString() + "\n@enduml");
    }

    private int createStep(String line, StringBuilder currentContent, List<String> includes, List<String> participants, List<Step> steps) {
        Map<String, Object> metadata = parseMetadata(line);
        int stepNumber = steps.size() + 1;
        if (metadata.containsKey("newPage")) {
            currentContent.setLength(0);
            currentContent.append("@startuml\n");
            addIncludes(currentContent, includes);
            addParticipants(currentContent, participants);
        }
        steps.add(new Step(stepNumber, metadata));
        return stepNumber;
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
