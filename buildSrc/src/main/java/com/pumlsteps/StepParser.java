package com.pumlsteps;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

    public ParsedPlantUmlFile parse(File sourceFile) throws IOException {
        String fileName = sourceFile.getName().replace(".puml", "");
        List<Step> steps = new ArrayList<>();
        List<String> lines = Files.readAllLines(sourceFile.toPath());
        StringBuilder currentContent = new StringBuilder();
        boolean inStep = false;
        int currentStep = -1;

        for (String line : lines) {
            if (isStepStart(line)) {
                if (inStep) {
                    throw new IllegalStateException("Nested steps are not allowed.");
                }
                Map<String, Object> metadata = parseMetadata(line);
                currentStep = (int) metadata.get("step");
                inStep = true;
                steps.add(new Step(currentStep, metadata));
            } else if (isStepEnd(line, currentStep)) {
                Step step = steps.get(steps.size() - 1); // Get the last added step
                step.setContent(currentContent.toString() + "\n@enduml");
                inStep = false;
                currentStep = -1;
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

}
