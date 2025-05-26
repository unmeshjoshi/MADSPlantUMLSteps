package com.pumlsteps.presentation;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class IndexGenerator {
    
    private static class PresentationInfo {
        String fileName;
        String title;
        String description;
        String icon;
        String category;
        String timeInfo;
        int slideCount;
        
        PresentationInfo(String fileName, String title, String description, String icon, String category, String timeInfo, int slideCount) {
            this.fileName = fileName;
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.category = category;
            this.timeInfo = timeInfo;
            this.slideCount = slideCount;
        }
    }
    
    public void generateIndex(File presentationDir, File outputFile) throws IOException {
        List<PresentationInfo> presentations = new ArrayList<>();
        int totalSlides = 0;
        int totalDiagrams = 0;
        
        // Scan all YAML files in the presentation directory
        File[] yamlFiles = presentationDir.listFiles((dir, name) -> name.endsWith(".yaml"));
        if (yamlFiles != null) {
            for (File yamlFile : yamlFiles) {
                PresentationInfo info = extractPresentationInfo(yamlFile);
                if (info != null) {
                    presentations.add(info);
                    totalSlides += info.slideCount;
                }
            }
        }
        
        // Count total diagrams from build/diagrams directory
        totalDiagrams = countDiagrams();
        
        // Sort presentations by category and then by title
        presentations.sort((a, b) -> {
            int categoryCompare = a.category.compareTo(b.category);
            return categoryCompare != 0 ? categoryCompare : a.title.compareTo(b.title);
        });
        
        // Generate the HTML
        generateIndexHtml(presentations, totalSlides, totalDiagrams, outputFile);
    }
    
    private PresentationInfo extractPresentationInfo(File yamlFile) throws IOException {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> yamlData = yaml.loadAs(new FileReader(yamlFile), Map.class);
            
            String title = (String) yamlData.get("title");
            if (title == null) {
                title = yamlFile.getName().replace(".yaml", "").replace("-", " ");
                title = title.substring(0, 1).toUpperCase() + title.substring(1);
            }
            
            // Count slides
            int slideCount = countSlides(yamlData);
            
            // Extract metadata from YAML or infer from content
            String fileName = yamlFile.getName().replace(".yaml", ".html");
            String description = extractOrGenerateDescription(yamlData, title);
            String icon = extractOrGenerateIcon(yamlData, title);
            String category = extractOrGenerateCategory(yamlData, title);
            String timeInfo = getTimeInfo(slideCount);
            
            return new PresentationInfo(fileName, title, description, icon, category, timeInfo, slideCount);
            
        } catch (Exception e) {
            System.err.println("Error processing " + yamlFile.getName() + ": " + e.getMessage());
            return null;
        }
    }
    
    private int countSlides(Map<String, Object> yamlData) {
        // Check if it has sections structure
        Object sectionsObj = yamlData.get("sections");
        if (sectionsObj != null) {
            List<Map<String, Object>> sections = (List<Map<String, Object>>) sectionsObj;
            int totalSlides = 0;
            for (Map<String, Object> section : sections) {
                List<Map<String, Object>> slides = (List<Map<String, Object>>) section.get("slides");
                if (slides != null) {
                    totalSlides += slides.size();
                }
            }
            return totalSlides;
        }
        
        // Simple slides structure
        Object slidesObj = yamlData.get("slides");
        if (slidesObj instanceof List) {
            return ((List<?>) slidesObj).size();
        }
        
        return 0;
    }
    
    private String extractOrGenerateDescription(Map<String, Object> yamlData, String title) {
        // Check if description is explicitly provided in YAML
        String description = (String) yamlData.get("description");
        if (description != null && !description.trim().isEmpty()) {
            return description;
        }
        
        // Fallback to generic description if not provided
        return "Interactive presentation covering " + title.toLowerCase() + " concepts with step-by-step diagrams.";
    }
    
    private String extractOrGenerateIcon(Map<String, Object> yamlData, String title) {
        // Check if icon is explicitly provided in YAML
        String icon = (String) yamlData.get("icon");
        if (icon != null && !icon.trim().isEmpty()) {
            return icon;
        }
        
        // Default icon if not provided
        return "fas fa-diagram-project";
    }
    
    private String extractOrGenerateCategory(Map<String, Object> yamlData, String title) {
        // Check if category is explicitly provided in YAML
        String category = (String) yamlData.get("category");
        if (category != null && !category.trim().isEmpty()) {
            return category;
        }
        
        // Default category if not provided
        return "General";
    }
    

    
    private String getTimeInfo(int slideCount) {
        if (slideCount > 30) {
            return "Extended Session";
        } else if (slideCount > 15) {
            return "Full Session";
        } else {
            return "Quick Overview";
        }
    }
    
    private int countDiagrams() {
        File diagramsDir = new File("build/diagrams");
        if (!diagramsDir.exists()) {
            return 0;
        }
        
        int count = 0;
        count += countSvgFiles(diagramsDir);
        return count;
    }
    
    private int countSvgFiles(File dir) {
        int count = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += countSvgFiles(file);
                } else if (file.getName().endsWith(".svg")) {
                    count++;
                }
            }
        }
        return count;
    }
    
    private void generateIndexHtml(List<PresentationInfo> presentations, int totalSlides, int totalDiagrams, File outputFile) throws IOException {
        StringBuilder html = new StringBuilder();
        
        // Generate the complete HTML
        html.append(getHtmlHeader());
        html.append(getHtmlBody(presentations, totalSlides, totalDiagrams));
        html.append(getHtmlFooter());
        
        // Write to file
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(html.toString());
        }
    }
    
    private String getHtmlHeader() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MADSPlantUMLSteps - Presentation Index</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=Poppins:wght@500;600;700&display=swap" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">
    <style>
        :root {
            --primary-color: #1a365d;
            --primary-dark: #0f172a;
            --primary-light: #e2e8f0;
            --accent-color: #475569;
            --accent-light: #f8fafc;
            --text-primary: #0f172a;
            --text-secondary: #475569;
            --text-light: #94a3b8;
            --background: #f8fafc;
            --surface: #ffffff;
            --surface-alt: #f1f5f9;
            --border: #e2e8f0;
            --border-dark: #cbd5e0;
            --shadow-sm: 0 1px 2px 0 rgba(15, 23, 42, 0.05);
            --shadow-md: 0 4px 6px -1px rgba(15, 23, 42, 0.1), 0 2px 4px -1px rgba(15, 23, 42, 0.06);
            --shadow-lg: 0 10px 15px -3px rgba(15, 23, 42, 0.1), 0 4px 6px -2px rgba(15, 23, 42, 0.05);
            --gradient-primary: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Inter', sans-serif;
            background-color: var(--background);
            color: var(--text-primary);
            line-height: 1.6;
            min-height: 100vh;
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
            padding: 2rem;
        }

        .header {
            text-align: center;
            margin-bottom: 3rem;
            padding: 3rem 0;
            background: var(--gradient-primary);
            border-radius: 16px;
            color: white;
            box-shadow: var(--shadow-lg);
        }

        .header h1 {
            font-family: 'Poppins', sans-serif;
            font-size: 3rem;
            font-weight: 700;
            margin-bottom: 1rem;
            text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
        }

        .header p {
            font-size: 1.2rem;
            opacity: 0.9;
            max-width: 600px;
            margin: 0 auto;
        }

        .presentations-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
            gap: 2rem;
            margin-bottom: 3rem;
        }

        .presentation-card {
            background: var(--surface);
            border-radius: 12px;
            padding: 2rem;
            box-shadow: var(--shadow-md);
            border: 1px solid var(--border);
            transition: all 0.3s ease;
            text-decoration: none;
            color: inherit;
            display: block;
        }

        .presentation-card:hover {
            transform: translateY(-4px);
            box-shadow: var(--shadow-lg);
            border-color: var(--primary-color);
        }

        .card-icon {
            width: 60px;
            height: 60px;
            background: var(--gradient-primary);
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            margin-bottom: 1.5rem;
            color: white;
            font-size: 1.5rem;
        }

        .card-title {
            font-family: 'Poppins', sans-serif;
            font-size: 1.5rem;
            font-weight: 600;
            margin-bottom: 0.75rem;
            color: var(--text-primary);
        }

        .card-description {
            color: var(--text-secondary);
            margin-bottom: 1.5rem;
            line-height: 1.6;
        }

        .card-meta {
            display: flex;
            align-items: center;
            gap: 1rem;
            font-size: 0.9rem;
            color: var(--text-light);
            flex-wrap: wrap;
        }

        .card-meta i {
            color: var(--primary-color);
        }

        .stats-section {
            background: var(--surface);
            border-radius: 12px;
            padding: 2rem;
            box-shadow: var(--shadow-md);
            border: 1px solid var(--border);
            margin-bottom: 2rem;
        }

        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 2rem;
        }

        .stat-item {
            text-align: center;
        }

        .stat-number {
            font-size: 2.5rem;
            font-weight: 700;
            color: var(--primary-color);
            margin-bottom: 0.5rem;
        }

        .stat-label {
            color: var(--text-secondary);
            font-weight: 500;
        }

        .footer {
            text-align: center;
            padding: 2rem 0;
            color: var(--text-light);
            border-top: 1px solid var(--border);
            margin-top: 3rem;
        }

        .footer a {
            color: var(--primary-color);
            text-decoration: none;
        }

        .footer a:hover {
            text-decoration: underline;
        }

        @media (max-width: 768px) {
            .container {
                padding: 1rem;
            }

            .header h1 {
                font-size: 2rem;
            }

            .header p {
                font-size: 1rem;
            }

            .presentations-grid {
                grid-template-columns: 1fr;
                gap: 1.5rem;
            }

            .stats-grid {
                grid-template-columns: repeat(2, 1fr);
            }
        }
    </style>
</head>
<body>
""";
    }
    
    private String getHtmlBody(List<PresentationInfo> presentations, int totalSlides, int totalDiagrams) {
        StringBuilder body = new StringBuilder();
        
        body.append("""
    <div class="container">
        <div class="header">
            <h1><i class="fas fa-diagram-project"></i> MADSPlantUMLSteps</h1>
            <p>Interactive presentations for understanding distributed systems concepts through PlantUML diagrams</p>
        </div>

        <div class="stats-section">
            <div class="stats-grid">
                <div class="stat-item">
                    <div class="stat-number">""").append(presentations.size()).append("""
</div>
                    <div class="stat-label">Presentations</div>
                </div>
                <div class="stat-item">
                    <div class="stat-number">""").append(totalDiagrams).append("""
</div>
                    <div class="stat-label">Diagrams</div>
                </div>
                <div class="stat-item">
                    <div class="stat-number">""").append(totalSlides).append("""
</div>
                    <div class="stat-label">Total Slides</div>
                </div>
                <div class="stat-item">
                    <div class="stat-number">Interactive</div>
                    <div class="stat-label">Navigation</div>
                </div>
            </div>
        </div>

        <div class="presentations-grid">
""");
        
        // Generate cards for each presentation
        for (PresentationInfo presentation : presentations) {
            body.append("""
            <a href=\"""").append(presentation.fileName).append("""
" class="presentation-card">
                <div class="card-icon">
                    <i class=\"""").append(presentation.icon).append("""
\"></i>
                </div>
                <div class="card-title">""").append(presentation.title).append("""
</div>
                <div class="card-description">
                    """).append(presentation.description).append("""

                </div>
                <div class="card-meta">
                    <span><i class="fas fa-tag"></i> """).append(presentation.category).append("""
</span>
                    <span><i class="fas fa-clock"></i> """).append(presentation.timeInfo).append("""
</span>
                    <span><i class="fas fa-list"></i> """).append(presentation.slideCount).append("""
 slides</span>
                </div>
            </a>
""");
        }
        
        body.append("""
        </div>
""");
        
        return body.toString();
    }
    
    private String getHtmlFooter() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new Date());
        
        return """
        <div class="footer">
            <p>
                Built with <i class="fas fa-heart" style="color: #e53e3e;"></i> using 
                <a href="https://plantuml.com/" target="_blank">PlantUML</a> and 
                <a href="https://gradle.org/" target="_blank">Gradle</a>
            </p>
            <p style="margin-top: 0.5rem; font-size: 0.9rem;">
                Last updated: <span id="build-time">""" + timestamp + """
</span>
            </p>
        </div>
    </div>

    <script>
        // Load build time from version.txt
        fetch('version.txt')
            .then(response => response.text())
            .then(data => {
                const buildTime = data.trim().replace('Built on: ', '');
                document.getElementById('build-time').textContent = buildTime;
            })
            .catch(() => {
                // Keep the hardcoded timestamp if version.txt is not available
            });

        // Add smooth scrolling for any internal links
        document.querySelectorAll('a[href^="#"]').forEach(anchor => {
            anchor.addEventListener('click', function (e) {
                e.preventDefault();
                document.querySelector(this.getAttribute('href')).scrollIntoView({
                    behavior: 'smooth'
                });
            });
        });
    </script>
</body>
</html>
""";
    }
} 