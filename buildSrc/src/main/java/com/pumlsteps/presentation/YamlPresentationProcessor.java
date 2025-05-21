package com.pumlsteps.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.pumlsteps.PptGenerator;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class YamlPresentationProcessor {
    private final File projectDir;
    private final ObjectMapper yamlMapper;

    public YamlPresentationProcessor(File projectDir) {
        this.projectDir = projectDir;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public void processYamlToPresentation(File yamlFile, File outputFile) {
        try {
            // Read and parse YAML
            PresentationConfig config = yamlMapper.readValue(yamlFile, PresentationConfig.class);
            
            // Create PPT
            PptGenerator generator = new PptGenerator();

            // Process slides directly if available
            if (config.getSlides() != null && !config.getSlides().isEmpty()) {
                for (SlideConfig slide : config.getSlides()) {
                    handleSlide(slide, generator);
                }
            }
            
            // Process sections if available
            if (config.getSections() != null && !config.getSections().isEmpty()) {
                for (SectionConfig section : config.getSections()) {
                    // Process each slide in the section
                    if (section.getSlides() != null) {
                        for (SlideConfig slide : section.getSlides()) {
                            handleSlide(slide, generator);
                        }
                    }
                }
            }

            // Save the presentation
            generator.save(outputFile.getAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException("Error processing YAML presentation: " + e.getMessage(), e);
        }
    }

    private void handleSlide(SlideConfig slide, PptGenerator generator) {
        switch (slide.getType().toLowerCase()) {
            case "text":
                createTextSlide(slide, generator);
                break;
            case "diagram":
                createDiagramSlide(slide, generator);
                break;
            default:
                throw new IllegalArgumentException("Unknown slide type: " + slide.getType());
        }
    }

    private void createTextSlide(SlideConfig slide, PptGenerator generator) {
        generator.addTextSlide(slide.getTitle(), slide.getBullets());
    }

    private void createDiagramSlide(SlideConfig slide, PptGenerator generator) {
        if (slide.getDiagramRef() != null) {
            String diagramPath = slide.getDiagramRef();
            File diagramFile = new File(projectDir, String.format("build/diagrams/%s/", diagramPath));
            File[] steps = diagramFile.listFiles((dir, name) -> name.endsWith(".png"));
            
            if (steps != null && steps.length > 0) {
                createStepsSlides(steps, slide, generator);
            }
            // Always add the base diagram if no steps exist
            File pngFile = new File(diagramFile, diagramPath + ".png");
            if (pngFile.exists()) {
                generator.addStepSlide("", slide.getTitle(), pngFile);
            }
        }
    }

    private void createStepsSlides(File[] steps, SlideConfig slide, PptGenerator generator) {
        // Sort steps by number suffix
        Arrays.sort(steps, Comparator.comparingInt(step -> {
            String name = step.getName();
            return Integer.parseInt(name.substring(name.lastIndexOf("step") + 4, name.lastIndexOf(".png")));
        }));
        
        // Add a slide for each step
        for (File step : steps) {
            generator.addStepSlide("", slide.getTitle(), step);
        }
    }
}
