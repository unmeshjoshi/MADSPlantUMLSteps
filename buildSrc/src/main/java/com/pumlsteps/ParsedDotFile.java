package com.pumlsteps;

import java.util.List;

public class ParsedDotFile {
    private final List<Step> steps;

    public ParsedDotFile(List<Step> steps) {
        this.steps = steps;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public int getStepCount() {
        return steps.size();
    }
} 