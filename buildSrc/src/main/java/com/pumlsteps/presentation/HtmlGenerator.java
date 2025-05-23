package com.pumlsteps.presentation;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtmlGenerator {
    private void generateBulletList(StringBuilder htmlContent, List<String> bullets) {
        if (bullets == null || bullets.isEmpty()) {
            return;
        }
        htmlContent.append("<ul>");
        for (String bullet : bullets) {
            htmlContent.append("<li>").append(bullet).append("</li>");
        }
        htmlContent.append("</ul>");
    }

    private void appendHtmlHeader(StringBuilder htmlContent, String title) {
        htmlContent.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
                .append("<title>").append(title).append("</title>")
                .append("<link rel=\"stylesheet\" href=\"styles.css\">")
                .append("<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">")
                .append("<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>")
                .append("<link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=Poppins:wght@500;600;700&display=swap\" rel=\"stylesheet\">")
                .append("<link href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css\" rel=\"stylesheet\">")
                .append("</head><body>");
    }

    private void appendNavigationControls(StringBuilder htmlContent) {
        htmlContent.append("<div class='controls'>")
                .append("<button id='prev-button' onclick='prevStep()' disabled>")
                .append("<i class='fas fa-chevron-left'></i>")
                .append("<span>Previous</span>")
                .append("</button>")
                .append("<button id='next-button' onclick='nextStep()'>")
                .append("<span>Next</span>")
                .append("<i class='fas fa-chevron-right'></i>")
                .append("</button>")
                .append("</div>");
    }

    private void generateDiagramContent(StringBuilder htmlContent, Map<String, Object> slide) {
        String fullSizeClass = !slide.containsKey("bullets") ? " full-size" : "";
        htmlContent.append("<div class='diagram-slide-content" + fullSizeClass + "'>");
        
        generateDiagramContainer(htmlContent, (String) slide.get("diagramRef"));
        
        if (slide.containsKey("bullets")) {
            htmlContent.append("<div class='diagram-bullets'>");
            generateBulletList(htmlContent, (List<String>) slide.get("bullets"));
            htmlContent.append("</div>");
        }
        
        htmlContent.append("</div>");
    }

    private void generateDiagramContainer(StringBuilder htmlContent, String diagramRef) {
        htmlContent.append("<div class='diagram-container'>");
        
        // First check if the diagram is directly in the diagrams directory
        File diagramDir = new File("build/diagrams/" + diagramRef);
        File[] stepFiles = diagramDir.listFiles((dir, name) -> name.endsWith(".svg"));
        String relativePath = "../diagrams/" + diagramRef + "/";
        
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
            System.out.println("Found " + stepFiles.length + " SVG files for " + diagramRef);
            for (File stepFile : stepFiles) {
                String stepName = stepFile.getName();
                String stepIndex = stepName.replaceAll("[^0-9]", "");
                // If there's no step index (e.g., for files like single_phase_execution.svg), use "1"
                if (stepIndex.isEmpty()) {
                    stepIndex = "1";
                }
                
                htmlContent.append("<div class='step' style='display: none;' step_index='")
                        .append(stepIndex)
                        .append("'><img src='")
                        .append(relativePath)
                        .append(stepName)
                        .append("' alt='")
                        .append(diagramRef)
                        .append(" ")
                        .append(stepName)
                        .append("'></div>");
            }
        } else {
            System.err.println("WARNING: No SVG files found for diagram: " + diagramRef + " in " + diagramDir.getAbsolutePath());
            // Add a placeholder message in the HTML
            htmlContent.append("<div class='step' style='display: block;'><p class='error-message'>No diagrams found for: ")
                    .append(diagramRef)
                    .append("</p></div>");
        }
        htmlContent.append("</div>");
    }

    private List<Map<String, Object>> getSectionsFromYaml(Map<String, Object> yamlData) {
        Object sectionsObj = yamlData.get("sections");
        if (sectionsObj != null) {
            List<Map<String, Object>> sections = (List<Map<String, Object>>) sectionsObj;
            for (Map<String, Object> section : sections) {
                Object slidesObj = section.get("slides");
                if (slidesObj instanceof List) {
                    section.put("slides", slidesObj);
                }
            }
            return sections;
        }
        
        List<Map<String, Object>> sections = new ArrayList<>();
        Map<String, Object> defaultSection = new HashMap<>();
        defaultSection.put("title", "Presentation");
        Object slides = yamlData.get("slides");
        if (slides instanceof List) {
            defaultSection.put("slides", slides);
        }
        sections.add(defaultSection);
        return sections;
    }

    private int countTotalSlides(List<Map<String, Object>> sections) {
        int totalSlides = 0;
        for (Map<String, Object> section : sections) {
            List<Map<String, Object>> sectionSlides = (List<Map<String, Object>>) section.get("slides");
            if (sectionSlides != null) {
                totalSlides += sectionSlides.size();
            }
        }
        return totalSlides;
    }

    public void generateHtmlFromYaml(File yamlFile, File outputHtmlFile) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> yamlData = yaml.loadAs(new FileReader(yamlFile), Map.class);
        StringBuilder htmlContent = new StringBuilder();
        
        appendHtmlHeader(htmlContent, (String) yamlData.get("title"));
        htmlContent.append("<div class='slide-container'>")
                .append("<div class='progress-bar'><div class='progress-indicator'></div></div>")
                .append("<div class='section-info' id='current-section'></div>");

        List<Map<String, Object>> sections = getSectionsFromYaml(yamlData);
        int totalSlides = countTotalSlides(sections);

        htmlContent.append("<div id='presentation-data' data-total-slides='").append(totalSlides).append("'></div>");

        for (Map<String, Object> section : sections) {
            String sectionTitle = (String) section.get("title");
            if (sectionTitle != null) {
                htmlContent.append("<div class='slide section-separator' data-section='").append(sectionTitle).append("'>")
                        .append("<div class='section-title'>")
                        .append("<h1>").append(sectionTitle).append("</h1>")
                        .append("</div>")
                        .append("</div>");
            }

            List<Map<String, Object>> sectionSlides = (List<Map<String, Object>>) section.get("slides");
            if (sectionSlides == null) continue;

            for (Map<String, Object> slide : sectionSlides) {
                htmlContent.append("<div class='slide' data-section='").append(sectionTitle).append("'>")
                        .append("<div class='title-container'>")
                        .append("<h2>")
                        .append(slide.get("title"))
                        .append("</h2>")
                        .append(slide.containsKey("notes") ? String.format(
                                "<div class='title-notes'><i class='fas fa-sticky-note'></i><span>%s</span></div>",
                                slide.get("notes")
                        ) : "")
                        .append("</div>")
                        .append("<div class='content-container'>");

                if ("text".equals(slide.get("type"))) {
                    generateBulletList(htmlContent, (List<String>) slide.get("bullets"));
                } else if ("diagram".equals(slide.get("type"))) {
                    generateDiagramContent(htmlContent, slide);
                }
                htmlContent.append("</div></div>"); 
            }
        }

        htmlContent.append("</div>"); 
        appendNavigationControls(htmlContent);
        
        htmlContent.append("<script src='presentation.js'></script>")
                .append("</body></html>");

        try (FileWriter writer = new FileWriter(outputHtmlFile)) {
            writer.write(htmlContent.toString());
        }
    }
}