package com.pumlsteps;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StepParser {
    private static final Pattern STEP_START_PATTERN = Pattern.compile("'\\s*\\[step(?:\\d+)?(?:\\s*\\{(.*)\\})?\\]");
    private static final Pattern COMMON_START_PATTERN = Pattern.compile("'\\s*\\[common\\]");
    private static final Pattern COMMON_END_PATTERN = Pattern.compile("'\\s*\\[/common\\]");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isStepStart(String line) {
        return STEP_START_PATTERN.matcher(line.trim()).matches();
    }
    
    public boolean isCommonStart(String line) {
        return COMMON_START_PATTERN.matcher(line.trim()).matches();
    }
    
    public boolean isCommonEnd(String line) {
        return COMMON_END_PATTERN.matcher(line.trim()).matches();
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
        List<String> lifelines = new ArrayList<>();
        List<String> commonDefinitions = new ArrayList<>();

        List<String> lines = sourceFile.readLines();
        StringBuilder currentContent = new StringBuilder();
        StringBuilder stepContent = new StringBuilder(); // Track content added to current step
        boolean inCommonBlock = false;
        
        for (String line : lines) {
            if (isCommonStart(line)) {
                inCommonBlock = true;
                // Don't add the marker line to content
                
            } else if (isCommonEnd(line)) {
                inCommonBlock = false;
                // Don't add the marker line to content
                
            } else if (inCommonBlock) {
                // Store common definitions for reuse in all steps
                commonDefinitions.add(line);
                // Also add to current content (for steps that don't have newPage)
                currentContent.append(line).append("\n");
                
            } else if (line.trim().startsWith("!include")) {
                addInclude(line, includes, currentContent);

            } else if (isTopLevelLifelineDeclaration(line)) {
                lifelines.add(line);
                currentContent.append(line).append("\n");

            } else if (isStepStart(line)) {
                endPreviousStep(steps, currentContent, stepContent);
                createStep(line, currentContent, includes, lifelines, commonDefinitions, steps);
                stepContent.setLength(0); // Reset step content tracker

            } else {
                currentContent.append(line).append("\n"); // Append lines outside step tags
                stepContent.append(line).append("\n"); // Track step-specific content
            }
        }

        //last step content.
        if (!steps.isEmpty()) {
          endPreviousStep(steps, currentContent, stepContent);
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

    private static void endPreviousStep(List<Step> steps, StringBuilder currentContent, StringBuilder stepContent) {
        if (steps.isEmpty()) {
            return;
        }
        Step step = steps.get(steps.size() - 1); // Get the last added step
        String content = currentContent.toString();
        
        // For pure note slides, remove lifelines only when the accumulated content
        // does not still contain sequence activity that depends on them.
        if (isNoteOnlyStep(stepContent.toString()) && hasLifelines(content) && !hasSequenceActivity(content)) {
            // Remove lifeline definitions for note-only steps
            content = removeLifelines(content);
        }
        
        // Only add @enduml if it's not already present
        if (!content.trim().endsWith("@enduml")) {
            content += "\n@enduml";
        }
        step.setContent(content);
    }
    
    private static boolean isNoteOnlyStep(String stepContent) {
        String[] lines = stepContent.split("\n");
        boolean hasNote = false;
        boolean hasSequenceElements = false;
        boolean hasNoteAcross = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Skip step markers and empty lines
            if (trimmed.isEmpty() || trimmed.startsWith("' [") || trimmed.startsWith("'[")) {
                continue;
            }
            
            if (trimmed.startsWith("note across")) {
                hasNoteAcross = true;
                hasNote = true;
            } else if (trimmed.startsWith("note ") || trimmed.equals("end note")) {
                hasNote = true;
            } else if (trimmed.contains("->") || trimmed.contains("-->") || 
                      trimmed.startsWith("group ") || trimmed.equals("end")) {
                hasSequenceElements = true;
            }
        }
        
        // Don't remove participants if using note across (it requires participants)
        return hasNote && !hasSequenceElements && !hasNoteAcross;
    }
    
    private static boolean hasLifelines(String content) {
        return content.contains("participant ") || content.contains("actor ");
    }

    private static boolean hasSequenceActivity(String content) {
        String[] lines = content.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("' [") || trimmed.startsWith("'[")) {
                continue;
            }

            if (trimmed.contains("->") || trimmed.contains("-->") ||
                trimmed.contains("<--") || trimmed.contains("->x") ||
                trimmed.contains("-->>") || trimmed.contains("<<--") ||
                trimmed.startsWith("group ") || trimmed.equals("end")) {
                return true;
            }
        }

        return false;
    }
    
    private static String removeLifelines(String content) {
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            if (!isTopLevelLifelineDeclaration(line)) {
                result.append(line).append("\n");
            }
        }
        
        return result.toString();
    }

    private int createStep(String line, StringBuilder currentContent, List<String> includes, List<String> lifelines, List<String> commonDefinitions, List<Step> steps) {
        Map<String, Object> metadata = parseMetadata(line);
        int stepNumber = steps.size() + 1;
        if (metadata.containsKey("newPage")) {
            currentContent.setLength(0);
            currentContent.append("@startuml\n");
            addIncludes(currentContent, includes);
            addLifelines(currentContent, lifelines);
            addCommonDefinitions(currentContent, commonDefinitions);
        }
        steps.add(new Step(stepNumber, metadata));
        return stepNumber;
    }

    private void addLifelines(StringBuilder currentContent, List<String> lifelines) {
        for (String lifeline : lifelines) {
            currentContent.append(lifeline).append("\n");
        }
    }
    
    private void addCommonDefinitions(StringBuilder currentContent, List<String> commonDefinitions) {
        for (String commonDef : commonDefinitions) {
            currentContent.append(commonDef).append("\n");
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

    private static boolean isTopLevelLifelineDeclaration(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("participant ") || trimmed.startsWith("actor ");
    }

}
