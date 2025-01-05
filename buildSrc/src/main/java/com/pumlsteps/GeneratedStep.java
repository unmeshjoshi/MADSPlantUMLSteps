package com.pumlsteps;

import java.io.File;

public class GeneratedStep {
    private final Step sourceStep;
    private final File pngFile;

    public GeneratedStep(Step sourceStep, File pngFile) {
        this.sourceStep = sourceStep;
        this.pngFile = pngFile;
    }

    public static File pngFile(Step sourceStep, File outputDir) {
        return new File(outputDir, sourceStep.pngFileName());
    }

    public static File pumlFile(Step sourceStep, File outputDir) {
        return new File(outputDir, sourceStep.pumlFileName());
    }

    public String getTitle() {
        return sourceStep.getTitle();
    }

    public File getPngFile() {
        return pngFile;
    }
}