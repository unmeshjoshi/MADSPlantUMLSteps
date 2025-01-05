package com.pumlsteps;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Step {
    private final int stepNumber;
    private final Map<String, Object> metadata;
    private String content;
    private String filePath;

    public Step(int stepNumber, Map<String, Object> metadata) {
        this.stepNumber = stepNumber;
        this.metadata = metadata;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getTitle() {
        return metadata.getOrDefault("name", "Step " + this.getStepNumber()).toString();
    }

    @NotNull
    public String pumlFileName() {
        return "step" + getStepNumber() + ".puml";
    }

    @NotNull
    public String pngFileName() {
        return "step" + getStepNumber() + ".png";
    }
}
