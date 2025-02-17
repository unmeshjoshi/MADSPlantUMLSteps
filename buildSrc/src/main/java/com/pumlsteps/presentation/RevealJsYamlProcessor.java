package com.pumlsteps.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;

public class RevealJsYamlProcessor {
    private final File projectDir;
    private final ObjectMapper yamlMapper;

    public RevealJsYamlProcessor(File projectDir) {
        this.projectDir = projectDir;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public void processYamlToPresentation(File yamlFile, File outputFile) {
        try {
            // Read and parse YAML
            PresentationConfig config = yamlMapper.readValue(yamlFile, PresentationConfig.class);
            
            StringBuilder slides = new StringBuilder();
            for (SlideConfig slide : config.getSlides()) {
                slides.append(generateSlide(slide));
            }

            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/reveal.js@4.5.0/dist/reveal.css">
                    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/reveal.js@4.5.0/dist/theme/white.css">
                    <style>
                        .reveal .slides section {
                            height: 100%%;
                        }
                        .reveal .slides img {
                            max-height: 75vh;
                            width: auto;
                            margin: 0;
                            border: 1px solid #ddd;
                            box-shadow: 0 4px 8px rgba(0,0,0,0.1);
                        }
                        .step-title {
                            font-size: 24px;
                            margin-bottom: 20px;
                            color: #333;
                        }
                        .step-container {
                            position: relative;
                            height: 80vh;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                        }
                        .step-image {
                            position: absolute;
                            top: 50%%;
                            left: 50%%;
                            transform: translate(-50%%, -50%%);
                            opacity: 0;
                            transition: opacity 0.5s ease;
                        }
                        .step-image.visible {
                            opacity: 1;
                        }
                        .bullets {
                            text-align: left;
                            font-size: 24px;
                            margin: 20px auto;
                            max-width: 800px;
                        }
                        .bullets li {
                            margin: 10px 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="reveal">
                        <div class="slides">
                            %s
                        </div>
                    </div>
                    <script src="https://cdn.jsdelivr.net/npm/reveal.js@4.5.0/dist/reveal.js"></script>
                    <script>
                        Reveal.initialize({
                            hash: true,
                            width: '100%%',
                            height: '100%%',
                            margin: 0.1,
                            controls: true,
                            progress: true,
                            center: true,
                            transition: 'slide',
                            keyboard: {
                                32: null // Disable space bar navigation
                            }
                        });
                        
                        // Override the next fragment behavior
                        Reveal.configure({
                            fragments: true,
                            fragmentInURL: true
                        });

                        let ignoreNextFragmentNavigation = false;

                        Reveal.on('fragmentshown', event => {
                            const fragment = event.fragment;
                            if (fragment.classList.contains('step-image')) {
                                // Hide all step images
                                document.querySelectorAll('.step-image').forEach(img => {
                                    img.classList.remove('visible');
                                });
                                // Show only the current fragment
                                fragment.classList.add('visible');
                                
                                // Check if this is the last fragment
                                const isLastFragment = !fragment.nextElementSibling || !fragment.nextElementSibling.classList.contains('step-image');
                                if (isLastFragment) {
                                    // Set up one-time handler for next fragment navigation
                                    const handleNextFragment = () => {
                                        if (!ignoreNextFragmentNavigation) {
                                            ignoreNextFragmentNavigation = true;
                                            Reveal.next(); // Move to next slide
                                        }
                                        Reveal.removeEventListener('fragmentnext', handleNextFragment);
                                    };
                                    Reveal.addEventListener('fragmentnext', handleNextFragment);
                                }
                            }
                        });
                        
                        Reveal.on('fragmenthidden', event => {
                            const fragment = event.fragment;
                            if (fragment.classList.contains('step-image')) {
                                fragment.classList.remove('visible');
                                // Find and show the previous visible fragment if it exists
                                const prevFragment = fragment.previousElementSibling;
                                if (prevFragment && prevFragment.classList.contains('step-image')) {
                                    prevFragment.classList.add('visible');
                                }
                            }
                        });

                        Reveal.on('slidechanged', () => {
                            ignoreNextFragmentNavigation = false;
                        });
                    </script>
                </body>
                </html>
                """, 
                config.getTitle() != null ? config.getTitle() : "Presentation",
                slides.toString()
            );

            Files.writeString(outputFile.toPath(), htmlContent);

        } catch (Exception e) {
            throw new RuntimeException("Error processing YAML presentation: " + e.getMessage(), e);
        }
    }

    private String generateSlide(SlideConfig slide) throws Exception {
        return switch (slide.getType().toLowerCase()) {
            case "text" -> generateTextSlide(slide);
            case "diagram" -> generateDiagramSlide(slide);
            default -> throw new IllegalArgumentException("Unknown slide type: " + slide.getType());
        };
    }

    private String generateTextSlide(SlideConfig slide) {
        StringBuilder bullets = new StringBuilder();
        if (slide.getBullets() != null) {
            bullets.append("<ul class=\"bullets\">");
            for (String bullet : slide.getBullets()) {
                bullets.append(String.format("<li>%s</li>", bullet));
            }
            bullets.append("</ul>");
        }

        return String.format("""
            <section>
                <h2>%s</h2>
                %s
            </section>
            """,
            slide.getTitle(),
            bullets.toString()
        );
    }

    private String generateDiagramSlide(SlideConfig slide) throws Exception {
        if (slide.getDiagramRef() == null) {
            return "";
        }

        String diagramPath = slide.getDiagramRef();
        File diagramDir = new File(projectDir, String.format("build/diagrams/%s/", diagramPath));
        File[] steps = diagramDir.listFiles((dir, name) -> name.endsWith(".png"));

        StringBuilder content = new StringBuilder();
        content.append(String.format("<section><h2>%s</h2>", slide.getTitle()));

        if (steps != null && steps.length > 0) {
            // Sort steps by number suffix
            Arrays.sort(steps, Comparator.comparingInt(step -> {
                String name = step.getName();
                return Integer.parseInt(name.substring(name.lastIndexOf("step") + 4, name.lastIndexOf(".png")));
            }));

            content.append("<div class=\"step-container\">");
            
            // Add each step as a fragment
            for (int i = 0; i < steps.length; i++) {
                String base64Image = Base64.getEncoder().encodeToString(Files.readAllBytes(steps[i].toPath()));
                content.append(String.format("""
                    <div class="step-image fragment" data-fragment-index="%d">
                        <img src="data:image/png;base64,%s" alt="Step %d">
                    </div>
                    """,
                    i,
                    base64Image,
                    i + 1
                ));
            }
            
            content.append("</div>");
        } else {
            // Add the base diagram if no steps exist
            File pngFile = new File(diagramDir, diagramPath + ".png");
            if (pngFile.exists()) {
                String base64Image = Base64.getEncoder().encodeToString(Files.readAllBytes(pngFile.toPath()));
                content.append(String.format("""
                    <div class="step-container">
                        <img src="data:image/png;base64,%s" alt="%s">
                    </div>
                    """,
                    base64Image,
                    slide.getTitle()
                ));
            }
        }

        content.append("</section>");
        return content.toString();
    }
}
