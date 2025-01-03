package com.pumlsteps;

import java.util.List;

public class ParsedPlantUmlFile {
    private final String name; // Extracted from the filename
    private final List<Step> steps;

    public ParsedPlantUmlFile(String name, List<Step> steps) {
        this.name = name;
        this.steps = steps;
    }

    public String getName() {
        return name;
    }

    public List<Step> getSteps() {
        return steps;
    }
}
