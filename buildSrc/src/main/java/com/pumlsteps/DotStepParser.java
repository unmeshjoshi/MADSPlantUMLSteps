package com.pumlsteps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DotStepParser {
    
    // Pattern to match step markers in DOT files: // [step1 {"name":"Step Name"}]
    private static final Pattern STEP_PATTERN = Pattern.compile("//\\s*\\[step(\\d+)\\s*(\\{.*?\\})?\\s*\\]");
    private static final Pattern STEP_END_PATTERN = Pattern.compile("//\\s*\\[/step(\\d+)\\]");

    public ParsedDotFile parse(DotFile dotFile) {
        String content = dotFile.getContent();
        List<Step> steps = new ArrayList<>();
        
        String[] lines = content.split("\n");
        StringBuilder currentStepContent = new StringBuilder();
        boolean inStep = false;
        Map<String, Object> currentStepMetadata = null;
        int currentStepNumber = 0;
        
        // If no step markers found, treat entire file as one step
        boolean hasStepMarkers = content.contains("[step");
        
        if (!hasStepMarkers) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("name", "Complete Diagram");
            Step singleStep = new Step(1, metadata);
            singleStep.setContent(content);
            steps.add(singleStep);
            return new ParsedDotFile(steps);
        }
        
        for (String line : lines) {
            Matcher stepMatcher = STEP_PATTERN.matcher(line);
            Matcher stepEndMatcher = STEP_END_PATTERN.matcher(line);
            
            if (stepMatcher.find()) {
                // Start of a new step
                if (inStep) {
                    // Save previous step if we were in one
                    Step step = new Step(currentStepNumber, currentStepMetadata);
                    step.setContent(currentStepContent.toString().trim());
                    steps.add(step);
                }
                
                currentStepNumber = Integer.parseInt(stepMatcher.group(1));
                currentStepMetadata = extractStepMetadata(stepMatcher.group(2));
                currentStepContent = new StringBuilder();
                inStep = true;
                
                // Don't include the step marker line in the content
                continue;
            } else if (stepEndMatcher.find()) {
                // End of current step
                if (inStep) {
                    Step step = new Step(currentStepNumber, currentStepMetadata);
                    step.setContent(currentStepContent.toString().trim());
                    steps.add(step);
                    inStep = false;
                    currentStepContent = new StringBuilder();
                }
                // Don't include the end marker line in the content
                continue;
            }
            
            // Add line to current step content
            if (inStep) {
                if (currentStepContent.length() > 0) {
                    currentStepContent.append("\n");
                }
                currentStepContent.append(line);
            }
        }
        
        // Handle case where file ends without explicit step end marker
        if (inStep && currentStepContent.length() > 0) {
            Step step = new Step(currentStepNumber, currentStepMetadata);
            step.setContent(currentStepContent.toString().trim());
            steps.add(step);
        }
        
        return new ParsedDotFile(steps);
    }
    
    private Map<String, Object> extractStepMetadata(String jsonString) {
        Map<String, Object> metadata = new HashMap<>();
        
        if (jsonString == null || jsonString.trim().isEmpty()) {
            metadata.put("name", "Step");
            return metadata;
        }
        
        // Simple JSON parsing for the name field
        Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = namePattern.matcher(jsonString);
        
        if (matcher.find()) {
            metadata.put("name", matcher.group(1));
        } else {
            metadata.put("name", "Step");
        }
        
        return metadata;
    }
} 