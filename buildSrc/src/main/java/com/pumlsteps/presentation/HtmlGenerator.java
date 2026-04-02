package com.pumlsteps.presentation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HtmlGenerator {
    // Store total step count and current step index for use when rendering slide titles
    private ThreadLocal<Integer> totalStepsForCurrentDiagram = new ThreadLocal<>();
    private ThreadLocal<Integer> currentStepIndexForDiagram = new ThreadLocal<>();
    private final PresentationConfigResolver configResolver = new PresentationConfigResolver();
    
    private String generateBulletList(List<String> bullets) {
        if (bullets == null || bullets.isEmpty()) {
            return "";
        }
        
        StringBuilder html = new StringBuilder("<ul>");
        for (String bullet : bullets) {
            html.append("<li>").append(bullet).append("</li>");
        }
        html.append("</ul>");
        return html.toString();
    }
    
    private String loadTemplate(String templateName) throws IOException {
        return TemplateLoader.loadTemplate(templateName + ".html");
    }
    
    private String renderTemplate(String templateName, Map<String, Object> data) throws IOException {
        String template = loadTemplate(templateName);
        return TemplateLoader.processTemplate(template, data);
    }

    private String generateDiagramContent(SlideConfig slide) throws IOException {
        Map<String, Object> data = new HashMap<>();
        
        // Generate diagram container content
        String diagramContent = generateDiagramContainer(slide.getDiagramRef());
        data.put("diagramContent", diagramContent);
        
        // Handle bullets if present
        boolean hasBullets = false;
        String bulletsHtml = "";
        List<String> bullets = slide.getBullets();
        if (bullets != null) {
            hasBullets = bullets.stream().anyMatch(bullet -> bullet != null && !bullet.trim().isEmpty());
            if (hasBullets) {
                bulletsHtml = generateBulletList(bullets);
            }
        }
        
        // Set fullSize based on whether there are actual bullets
        data.put("fullSize", !hasBullets);
        data.put("hasBullets", hasBullets);
        data.put("bulletsHtml", bulletsHtml);
        
        // Store diagram step count for use in slide title
        String diagramRef = slide.getDiagramRef();
        if (diagramRef != null) {
            File diagramDir = new File("build/diagrams/" + diagramRef);
            File[] stepFiles = diagramDir.listFiles((dir, name) -> name.endsWith(".svg"));
            if (stepFiles != null) {
                totalStepsForCurrentDiagram.set(stepFiles.length);
            }
        }
        
        return renderTemplate("diagram_content", data);
    }

    private String generateDiagramContainer(String diagramRef) {
        StringBuilder htmlContent = new StringBuilder();
        
        // First check if the diagram is directly in the diagrams directory
        File diagramDir = new File("build/diagrams/" + diagramRef);
        File[] stepFiles = diagramDir.listFiles((dir, name) -> name.endsWith(".svg"));
        String relativePath = "../diagrams/" + diagramRef + "/";
        
        // Store total number of steps to determine if we should show step numbers in title
        int totalSteps = 0;
        
        // If not found, check if it's in a subdirectory (e.g., consensus/single_server_problem)
        if (stepFiles == null || stepFiles.length == 0) {
            // Try to find the directory by searching in subdirectories
            File diagramsRoot = new File("build/diagrams");
            for (File subdir : diagramsRoot.listFiles(File::isDirectory)) {
                // Check if the diagram is directly in this subdirectory
                File potentialDir = new File(subdir, diagramRef);
                if (potentialDir.exists() && potentialDir.isDirectory()) {
                    diagramDir = potentialDir;
                    stepFiles = diagramDir.listFiles((dir, name) -> name.endsWith(".svg"));
                    if (stepFiles != null && stepFiles.length > 0) {
                        System.out.println("Found SVG files for " + diagramRef + " in subdirectory: " + subdir.getName());
                        relativePath = "../diagrams/" + subdir.getName() + "/" + diagramRef + "/";
                        break;
                    }
                }
                
                // Check in deeper subdirectories (e.g., consensus/need_for_two_phases/single_phase_execution)
                File[] subdirContents = subdir.listFiles(File::isDirectory);
                if (subdirContents != null) {
                    for (File deeperSubdir : subdirContents) {
                        // Check if the diagram is in a subdirectory of this deeper subdirectory
                        File potentialDeeperDir = new File(deeperSubdir, diagramRef);
                        if (potentialDeeperDir.exists() && potentialDeeperDir.isDirectory()) {
                            diagramDir = potentialDeeperDir;
                            stepFiles = diagramDir.listFiles((dir, name) -> name.endsWith(".svg"));
                            if (stepFiles != null && stepFiles.length > 0) {
                                System.out.println("Found SVG files for " + diagramRef + " in deeper subdirectory: " + 
                                    subdir.getName() + "/" + deeperSubdir.getName() + "/" + diagramRef);
                                relativePath = "../diagrams/" + subdir.getName() + "/" + deeperSubdir.getName() + "/" + diagramRef + "/";
                                break;
                            }
                        }
                    }
                }
                
                // If we found files, no need to continue searching
                if (stepFiles != null && stepFiles.length > 0) {
                    break;
                }
            }
        }

        if (stepFiles != null && stepFiles.length > 0) {
            totalSteps = stepFiles.length;
            System.out.println("Found " + totalSteps + " SVG files for " + diagramRef);
            for (File stepFile : stepFiles) {
                String stepName = stepFile.getName();
                String stepIndex = stepName.replaceAll("[^0-9]", "");
                // If there's no step index (e.g., for files like single_phase_execution.svg), use "1"
                if (stepIndex.isEmpty()) {
                    stepIndex = "1";
                }
                
                // Create more descriptive alt text with diagram name and step number
                String altText = diagramRef.replace("_", " ");
                if (totalSteps > 1) {
                    altText += " - Step " + stepIndex;
                }
                
                htmlContent.append("<div class='step' style='display: none;' step_index='")
                        .append(stepIndex)
                        .append("'><img src='")
                        .append(relativePath)
                        .append(stepName)
                        .append("' alt='")
                        .append(altText)
                        .append("'></div>");
            }
        } else {
            System.err.println("WARNING: No SVG files found for diagram: " + diagramRef + " in " + diagramDir.getAbsolutePath());
            // Add a placeholder message in the HTML
            htmlContent.append("<div class='step' style='display: block;'><p class='error-message'>No diagrams found for: ")
                    .append(diagramRef)
                    .append("</p></div>");
        }
        
        return htmlContent.toString();
    }

    private List<SectionConfig> getSectionsFromConfig(PresentationConfig config) {
        if (config.getSections() != null && !config.getSections().isEmpty()) {
            return config.getSections();
        }

        List<SectionConfig> sections = new ArrayList<>();
        if (config.getSlides() != null && !config.getSlides().isEmpty()) {
            SectionConfig anonymousSection = new SectionConfig();
            anonymousSection.setTitle(null);
            anonymousSection.setSlides(config.getSlides());
            sections.add(anonymousSection);
        }
        return sections;
    }

    private int countTotalSlides(List<SectionConfig> sections) {
        int totalSlides = 0;
        for (SectionConfig section : sections) {
            List<SlideConfig> sectionSlides = section.getSlides();
            if (sectionSlides != null) {
                totalSlides += sectionSlides.size();
            }
        }
        return totalSlides;
    }

    public void generateHtmlFromYaml(File yamlFile, File outputHtmlFile) throws IOException {
        PresentationConfig config = configResolver.resolve(yamlFile);
        StringBuilder htmlContent = new StringBuilder();
        
        // Prepare header data
        Map<String, Object> headerData = new HashMap<>();
        headerData.put("title", config.getTitle());
        
        // Get sections and calculate total slides
        List<SectionConfig> sections = getSectionsFromConfig(config);
        int totalSlides = countTotalSlides(sections);
        headerData.put("totalSlides", totalSlides);
        
        // Render header template
        htmlContent.append(renderTemplate("header", headerData));
        
        // Process each section and its slides
        for (SectionConfig section : sections) {
            String sectionTitle = section.getTitle();
            if (sectionTitle != null && !sectionTitle.trim().isEmpty()) {
                Map<String, Object> sectionData = new HashMap<>();
                sectionData.put("sectionTitle", sectionTitle);
                htmlContent.append(renderTemplate("section", sectionData));
            }

            List<SlideConfig> sectionSlides = section.getSlides();
            if (sectionSlides == null) continue;

            for (SlideConfig slide : sectionSlides) {
                Map<String, Object> slideData = new HashMap<>();
                slideData.put("sectionTitle", sectionTitle);
                
                // Handle diagram slide titles with step numbers (only for multi-step diagrams)
                String slideTitle = slide.getTitle();
                if ("diagram".equals(slide.getType())) {
                    String diagramRef = slide.getDiagramRef();
                    File diagramDir = new File("build/diagrams/" + diagramRef);
                    File[] stepFiles = diagramDir.listFiles((dir, name) -> name.endsWith(".svg"));
                    
                    if (stepFiles != null && stepFiles.length > 1) {
                        // Extract step number from filename
                        for (File stepFile : stepFiles) {
                            String stepName = stepFile.getName();
                            String stepIndex = stepName.replaceAll("[^0-9]", "");
                            if (stepIndex.isEmpty()) {
                                stepIndex = "1";
                            }
                            
                            // If this is a multi-step diagram, add step number to title
                            if (Integer.parseInt(stepIndex) == 1) {
                                // Only modify the title for the first step to keep consistency
                                slideTitle = slideTitle + " - Step " + stepIndex;
                                break;
                            }
                        }
                    }
                }
                slideData.put("slideTitle", slideTitle);
                
                // Handle notes if present
                if (slide.getNotes() != null && !slide.getNotes().trim().isEmpty()) {
                    slideData.put("hasNotes", true);
                    slideData.put("notes", slide.getNotes());
                }
                
                // Generate content based on slide type
                String content;
                if ("text".equals(slide.getType())) {
                    content = generateBulletList(slide.getBullets());
                } else if ("diagram".equals(slide.getType())) {
                    content = generateDiagramContent(slide);
                } else {
                    content = "";
                }
                
                slideData.put("content", content);
                htmlContent.append(renderTemplate("slide", slideData));
            }
        }

        // Add navigation controls and footer
        htmlContent.append(renderTemplate("navigation", new HashMap<>()));
        htmlContent.append(renderTemplate("footer", new HashMap<>()));

        // Write the final HTML content to file
        try (FileWriter writer = new FileWriter(outputHtmlFile)) {
            writer.write(htmlContent.toString());
        }
    }
}
