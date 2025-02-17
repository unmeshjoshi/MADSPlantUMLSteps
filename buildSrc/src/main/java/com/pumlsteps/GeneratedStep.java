package com.pumlsteps;

import java.io.File;

public class GeneratedStep {
    private final Step sourceStep;
    private final File imageFile;

    public GeneratedStep(Step sourceStep, File imageFile) {
        this.sourceStep = sourceStep;
        this.imageFile = imageFile;
    }

    public static File imageFile(Step sourceStep, File outputDir) {
        return new File(outputDir, sourceStep.svgFileName());
    }

    public static File pumlFile(Step sourceStep, File outputDir) {
        return new File(outputDir, sourceStep.pumlFileName());
    }

    public String getTitle() {
        return sourceStep.getTitle();
    }

    public File getImageFile() {
        return imageFile;
    }
}