package com.pumlsteps;

import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PptGenerator {
    private XMLSlideShow ppt;
    private File pngDirectory;

    /**
     * Creates a new PowerPoint generator.
     */
    public PptGenerator() {
        ppt = new XMLSlideShow();
        ppt.setPageSize(new Dimension(1024, 768));
    }
    
    /**
     * Sets the directory containing pre-converted PNG files.
     * 
     * @param pngDirectory The directory containing PNG files
     */
    public void setPngDirectory(File pngDirectory) {
        this.pngDirectory = pngDirectory;
        System.out.println("PptGenerator using PNG directory: " + pngDirectory.getAbsolutePath());
    }

    /**
     * Adds a section and its associated slides to the PowerPoint.
     *
     * @param sectionName The name of the section.
     * @param steps       The list of steps in the section.
     */
    public void addSlides(String sectionName, List<GeneratedStep> steps) {
        System.out.println("Adding " + steps.size() + " slides for section: " + sectionName);
        
        // Add a separator slide for the section
        addSeparatorSlide(sectionName);
        
        // If we have a PNG directory with pre-converted files, use batch processing
        if (pngDirectory != null && pngDirectory.exists()) {
            List<File> svgFiles = new ArrayList<>();
            List<File> pngFiles = new ArrayList<>();
            
            // Collect SVG files and find corresponding PNG files
            for (GeneratedStep step : steps) {
                File svgFile = step.getImageFile();
                if (svgFile != null && svgFile.exists()) {
                    // Find the corresponding PNG file in the PNG directory
                    String pngFileName = svgFile.getName().substring(0, svgFile.getName().lastIndexOf('.')) + ".png";
                    File pngFile = new File(pngDirectory, pngFileName);
                    
                    if (pngFile.exists()) {
                        svgFiles.add(svgFile);
                        pngFiles.add(pngFile);
                    } else {
                        System.err.println("WARNING: Pre-converted PNG file not found for " + svgFile.getName());
                    }
                }
            }
            
            // Add all slides in batch
            if (!svgFiles.isEmpty()) {
                addStepSlidesInBatch(sectionName, svgFiles, pngFiles);
                return;
            }
        }
        
        // Fall back to individual processing if batch processing is not possible
        for (GeneratedStep step : steps) {
            addStepSlide(sectionName, step.getTitle(), step.getImageFile());
        }
    }

    /**
     * Adds a separator slide for a section.
     *
     * @param sectionName The name of the section.
     */
    private void addSeparatorSlide(String sectionName) {
        try {
            XSLFSlide slide = ppt.createSlide();
            XSLFTextBox textBox = slide.createTextBox();
            textBox.setText(sectionName);
            textBox.setAnchor(new Rectangle(50, 150, 860, 240)); // Center text in the slide
            textBox.setHorizontalCentered(true);
            textBox.setVerticalAlignment(VerticalAlignment.MIDDLE);
            textBox.getTextParagraphs().get(0).setBulletFontSize(36.0);
        } catch (Exception e) {
            throw new RuntimeException("Error adding separator slide: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a slide for an individual step.
     *
     * @param sectionName The section name.
     * @param title       The title for the step.
     * @param imageFile   The image file for the step diagram.
     */
    public void addStepSlide(String sectionName, String title, File imageFile) {
        try {
            // Get slide dimensions
            int slideWidth = ppt.getPageSize().width;
            int slideHeight = ppt.getPageSize().height;

            // Check if the image file exists
            if (!imageFile.exists()) {
                System.err.println("ERROR: Image file does not exist: " + imageFile.getAbsolutePath());
                return; // Skip this slide if the image doesn't exist
            }

            // Create a new slide
            XSLFSlide slide = ppt.createSlide();

            // Add the title
            XSLFTextBox titleBox = slide.createTextBox();
            titleBox.setAnchor(new Rectangle(50, 20, slideWidth - 100, 50));
            titleBox.setHorizontalCentered(true);

            // Create a text run to set font properties
            XSLFTextRun titleRun = titleBox.addNewTextParagraph().addNewTextRun();
            titleRun.setText(title);
            titleRun.setFontFamily("Bitter");
            titleRun.setFontSize(26.0);

            // Check if the file is an SVG file
            boolean isSvg = imageFile.getName().toLowerCase(Locale.ROOT).endsWith(".svg");

            if (isSvg) {
                // For SVG files, we should use a pre-converted PNG file
                File pngFile = null;
                
                // If we have a PNG directory, look for a corresponding PNG file
                if (pngDirectory != null && pngDirectory.exists()) {
                    String pngFileName = imageFile.getName().substring(0, imageFile.getName().lastIndexOf('.')) + ".png";
                    pngFile = new File(pngDirectory, pngFileName);
                    
                    if (!pngFile.exists()) {
                        System.err.println("WARNING: Pre-converted PNG file not found: " + pngFile.getAbsolutePath());
                        pngFile = null;
                    }
                }
                
                if (pngFile != null && pngFile.exists()) {
                    // Use the pre-converted PNG file
                    BufferedImage image = ImageIO.read(pngFile);
                    
                    if (image != null) {
                        // Calculate dimensions for the image
                        int imageY = 50;  // Below title
                        int imageHeight = slideHeight - imageY - 20;  // Leave margin at bottom

                        double scaleX = (double) (slideWidth - 200) / image.getWidth();
                        double scaleY = (double) imageHeight / image.getHeight();
                        double scale = Math.min(scaleX, scaleY);

                        int scaledWidth = (int) (image.getWidth() * scale);
                        int scaledHeight = (int) (image.getHeight() * scale);

                        // Center the image
                        int xPosition = (slideWidth - scaledWidth) / 2;
                        int yPosition = (slideHeight - scaledHeight) / 2;

                        // Read the PNG file bytes
                        byte[] imageBytes = Files.readAllBytes(pngFile.toPath());
                        
                        // Add the PNG image to the slide
                        XSLFPictureData pictureData = ppt.addPicture(imageBytes, XSLFPictureData.PictureType.PNG);
                        slide.createPicture(pictureData).setAnchor(new Rectangle(xPosition, yPosition, scaledWidth, scaledHeight));
                    } else {
                        System.err.println("ERROR: Failed to read PNG image for dimensions");
                        addImageWithDefaultDimensions(slide, imageFile, slideWidth, slideHeight);
                    }
                } else {
                    // If no pre-converted PNG file is available, fall back to creating one on-the-fly
                    System.err.println("WARNING: No pre-converted PNG file available for " + imageFile.getName() + ", creating one on-the-fly");
                    File tempPngFile = createTempPngFromSvg(imageFile);
                    
                    if (tempPngFile != null && tempPngFile.exists()) {
                        // Use the temporary PNG file
                        BufferedImage image = ImageIO.read(tempPngFile);
                        
                        if (image != null) {
                            // Calculate dimensions for the image
                            int imageY = 50;  // Below title
                            int imageHeight = slideHeight - imageY - 20;  // Leave margin at bottom

                            double scaleX = (double) (slideWidth - 200) / image.getWidth();
                            double scaleY = (double) imageHeight / image.getHeight();
                            double scale = Math.min(scaleX, scaleY);

                            int scaledWidth = (int) (image.getWidth() * scale);
                            int scaledHeight = (int) (image.getHeight() * scale);

                            // Center the image
                            int xPosition = (slideWidth - scaledWidth) / 2;
                            int yPosition = (slideHeight - scaledHeight) / 2;

                            // Read the PNG file bytes
                            byte[] imageBytes = Files.readAllBytes(tempPngFile.toPath());
                            
                            // Add the PNG image to the slide
                            XSLFPictureData pictureData = ppt.addPicture(imageBytes, XSLFPictureData.PictureType.PNG);
                            slide.createPicture(pictureData).setAnchor(new Rectangle(xPosition, yPosition, scaledWidth, scaledHeight));
                            
                            // Clean up the temporary file
                            tempPngFile.delete();
                        } else {
                            System.err.println("ERROR: Failed to read temporary PNG image for dimensions");
                            addImageWithDefaultDimensions(slide, imageFile, slideWidth, slideHeight);
                        }
                    } else {
                        System.err.println("ERROR: Failed to create temporary PNG from SVG");
                        addImageWithDefaultDimensions(slide, imageFile, slideWidth, slideHeight);
                    }
                }
            } else {
                // For non-SVG files, we can read the dimensions directly
                BufferedImage image = ImageIO.read(imageFile);
                
                if (image != null) {
                    // Calculate remaining area for image
                    int imageY = 50;  // Below title
                    int imageHeight = slideHeight - imageY - 20;  // Leave margin at bottom

                    double scaleX = (double) (slideWidth - 200) / image.getWidth();
                    double scaleY = (double) imageHeight / image.getHeight();
                    double scale = Math.min(scaleX, scaleY);

                    int scaledWidth = (int) (image.getWidth() * scale);
                    int scaledHeight = (int) (image.getHeight() * scale);

                    // Center the image
                    int xPosition = (slideWidth - scaledWidth) / 2;
                    int yPosition = (slideHeight - scaledHeight) / 2;

                    // Read the image file bytes
                    byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
                    
                    // Determine the picture type based on the file extension
                    XSLFPictureData.PictureType pictureType = getPictureType(imageFile.getName());
                    
                    XSLFPictureData pictureData = ppt.addPicture(imageBytes, pictureType);
                    slide.createPicture(pictureData).setAnchor(new Rectangle(xPosition, yPosition, scaledWidth, scaledHeight));
                } else {
                    System.err.println("ERROR: Failed to read image for dimensions");
                    addImageWithDefaultDimensions(slide, imageFile, slideWidth, slideHeight);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error adding step slide: " + e.getMessage(), e);
        }
    }

    /**
     * Adds an image to a slide with default dimensions.
     *
     * @param slide       The slide to add the image to
     * @param imageFile   The image file
     * @param slideWidth  The width of the slide
     * @param slideHeight The height of the slide
     */
    private void addImageWithDefaultDimensions(XSLFSlide slide, File imageFile, int slideWidth, int slideHeight) {
        try {
            // Use default dimensions
            int defaultWidth = 800;
            int defaultHeight = 600;
            
            // Center the image
            int xPosition = (slideWidth - defaultWidth) / 2;
            int yPosition = (slideHeight - defaultHeight) / 2;
            
            // Read the image file bytes
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            
            // Determine the picture type based on the file extension
            XSLFPictureData.PictureType pictureType = getPictureType(imageFile.getName());
            
            XSLFPictureData pictureData = ppt.addPicture(imageBytes, pictureType);
            slide.createPicture(pictureData).setAnchor(new Rectangle(xPosition, yPosition, defaultWidth, defaultHeight));
        } catch (Exception e) {
            System.err.println("ERROR: Failed to add image with default dimensions: " + e.getMessage());
        }
    }

    /**
     * Determines the picture type based on the file name.
     *
     * @param fileName The name of the file
     * @return The picture type
     */
    private XSLFPictureData.PictureType getPictureType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        
        switch (extension) {
            case "png":
                return XSLFPictureData.PictureType.PNG;
            case "jpg":
            case "jpeg":
                return XSLFPictureData.PictureType.JPEG;
            case "gif":
                return XSLFPictureData.PictureType.GIF;
            case "svg":
                try {
                    return XSLFPictureData.PictureType.SVG;
                } catch (Exception e) {
                    // If SVG is not supported, fall back to PNG
                    return XSLFPictureData.PictureType.PNG;
                }
            default:
                return XSLFPictureData.PictureType.PNG;
        }
    }

    /**
     * Adds a slide for an individual step.
     *
     * @param sectionName The section name.
     * @param title       The title for the step.
     * @param svgFile     The SVG image file for the step diagram.
     * @param pngFile     The pre-generated PNG file for dimension calculation.
     */
    public void addStepSlide(String sectionName, String title, File svgFile, File pngFile) {
        try {
            // Get slide dimensions
            int slideWidth = ppt.getPageSize().width;
            int slideHeight = ppt.getPageSize().height;

            // Verify the files exist
            if (!svgFile.exists() || !pngFile.exists()) {
                System.err.println("ERROR: Image files do not exist: SVG=" + svgFile.getAbsolutePath() + 
                                  ", PNG=" + pngFile.getAbsolutePath());
                return; // Skip this slide if the images don't exist
            }

            // Create a new slide
            XSLFSlide slide = ppt.createSlide();

            // Add the title
            XSLFTextBox titleBox = slide.createTextBox();
            titleBox.setAnchor(new Rectangle(50, 20, slideWidth - 100, 50));
            titleBox.setHorizontalCentered(true);

            // Create a text run to set font properties
            XSLFTextRun titleRun = titleBox.addNewTextParagraph().addNewTextRun();
            titleRun.setText(title);
            titleRun.setFontFamily("Bitter");
            titleRun.setFontSize(26.0);

            // Read the PNG for dimension calculation
            BufferedImage image = ImageIO.read(pngFile);
            if (image == null) {
                System.err.println("ERROR: Failed to read PNG image for dimensions: " + pngFile.getAbsolutePath());
                // Fall back to the original method
                addStepSlide(sectionName, title, svgFile);
                return;
            }
            
            // Calculate remaining area for image
            int imageY = 50;  // Below title
            int imageHeight = slideHeight - imageY - 20;  // Leave margin at bottom

            double scaleX = (double) (slideWidth - 200) / image.getWidth();
            double scaleY = (double) imageHeight / image.getHeight();
            double scale = Math.min(scaleX, scaleY);

            int scaledWidth = (int) (image.getWidth() * scale);
            int scaledHeight = (int) (image.getHeight() * scale);

            // Center the image
            int xPosition = (slideWidth - scaledWidth) / 2;
            int yPosition = (slideHeight - scaledHeight) / 2;

            // Read the SVG file bytes
            byte[] imageBytes = Files.readAllBytes(svgFile.toPath());
            
            // Add the SVG image to the slide
            XSLFPictureData.PictureType pictureType;
            try {
                pictureType = XSLFPictureData.PictureType.SVG;
            } catch (Exception e) {
                // If SVG is not supported, fall back to PNG
                System.out.println("SVG not supported by this PowerPoint version, using PNG");
                pictureType = XSLFPictureData.PictureType.PNG;
                imageBytes = Files.readAllBytes(pngFile.toPath());
            }
            
            XSLFPictureData pictureData = ppt.addPicture(imageBytes, pictureType);
            
            slide.createPicture(pictureData).setAnchor(new Rectangle(xPosition, yPosition, scaledWidth, scaledHeight));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error adding step slide: " + e.getMessage(), e);
        }
    }
    
    /**
     * Adds multiple slides for steps in a batch, using pre-generated PNG files for dimension calculation.
     *
     * @param title       The title for all steps.
     * @param svgFiles    The SVG image files for the step diagrams.
     * @param pngFiles    The pre-generated PNG files for dimension calculation.
     */
    public void addStepSlidesInBatch(String title, List<File> svgFiles, List<File> pngFiles) {
        if (svgFiles.size() != pngFiles.size()) {
            throw new IllegalArgumentException("SVG files and PNG files must have the same size");
        }
        
        System.out.println("Adding " + svgFiles.size() + " slides with batch-processed images");
        
        // Get slide dimensions
        int slideWidth = ppt.getPageSize().width;
        int slideHeight = ppt.getPageSize().height;
        
        for (int i = 0; i < svgFiles.size(); i++) {
            File svgFile = svgFiles.get(i);
            File pngFile = pngFiles.get(i);
            
            try {
                // Verify the files exist
                if (!svgFile.exists()) {
                    System.err.println("WARNING: SVG file does not exist: " + svgFile.getAbsolutePath());
                    continue; // Skip this slide if the SVG doesn't exist
                }
                
                if (!pngFile.exists()) {
                    System.err.println("WARNING: PNG file does not exist: " + pngFile.getAbsolutePath());
                    // Try to create a PNG from the SVG on-the-fly
                    pngFile = createTempPngFromSvg(svgFile);
                    if (pngFile == null) {
                        // Fall back to using just the SVG file
                        addStepSlide("", title, svgFile);
                        continue;
                    }
                }

                // Create a new slide
                XSLFSlide slide = ppt.createSlide();

                // Add the title
                XSLFTextBox titleBox = slide.createTextBox();
                titleBox.setAnchor(new Rectangle(50, 20, slideWidth - 100, 50));
                titleBox.setHorizontalCentered(true);

                // Create a text run to set font properties
                XSLFTextRun titleRun = titleBox.addNewTextParagraph().addNewTextRun();
                titleRun.setText(title);
                titleRun.setFontFamily("Bitter");
                titleRun.setFontSize(26.0);

                // Read the PNG for dimension calculation
                BufferedImage image = null;
                try {
                    image = ImageIO.read(pngFile);
                    if (image == null) {
                        throw new IOException("ImageIO.read returned null");
                    }
                } catch (Exception e) {
                    System.err.println("ERROR: Failed to read PNG image " + pngFile.getAbsolutePath() + ": " + e.getMessage());
                }
                
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    System.err.println("WARNING: Invalid PNG image or empty placeholder: " + pngFile.getAbsolutePath());
                    // Try to create a new PNG from the SVG
                    File tempPngFile = createTempPngFromSvg(svgFile);
                    if (tempPngFile != null) {
                        try {
                            image = ImageIO.read(tempPngFile);
                            if (image == null) {
                                throw new IOException("ImageIO.read returned null for newly created PNG");
                            }
                            // Update the pngFile reference to use this valid PNG
                            pngFile = tempPngFile;
                        } catch (Exception e) {
                            System.err.println("ERROR: Failed to read newly created PNG: " + e.getMessage());
                            tempPngFile.delete();
                        }
                    }
                    
                    // If we still don't have a valid image, use default dimensions
                    if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                        System.err.println("WARNING: Using default dimensions for slide " + (i+1));
                        addImageWithDefaultDimensions(slide, svgFile, slideWidth, slideHeight);
                        continue;
                    }
                }
                
                // Calculate remaining area for image
                int imageY = 80;  // Below title
                int imageHeight = slideHeight - imageY - 20;  // Leave margin at bottom

                double scaleX = (double) (slideWidth - 100) / image.getWidth();
                double scaleY = (double) imageHeight / image.getHeight();
                double scale = Math.min(scaleX, scaleY);

                int scaledWidth = (int) (image.getWidth() * scale);
                int scaledHeight = (int) (image.getHeight() * scale);

                // Center the image
                int xPosition = (slideWidth - scaledWidth) / 2;
                int yPosition = imageY + ((slideHeight - imageY - 20 - scaledHeight) / 2);

                try {
                    // First try to use the PNG file directly
                    byte[] pngBytes = Files.readAllBytes(pngFile.toPath());
                    XSLFPictureData pictureData = ppt.addPicture(pngBytes, XSLFPictureData.PictureType.PNG);
                    XSLFPictureShape picture = slide.createPicture(pictureData);
                    picture.setAnchor(new Rectangle(xPosition, yPosition, scaledWidth, scaledHeight));
                    System.out.println("Successfully added PNG image to slide " + (i+1));
                } catch (Exception e) {
                    System.err.println("ERROR: Failed to add PNG image to slide: " + e.getMessage());
                    
                    // If PNG fails, try SVG as a fallback
                    try {
                        byte[] svgBytes = Files.readAllBytes(svgFile.toPath());
                        XSLFPictureData.PictureType pictureType;
                        try {
                            pictureType = XSLFPictureData.PictureType.SVG;
                        } catch (Exception ex) {
                            System.err.println("SVG not supported by this PowerPoint version");
                            // If we get here, both PNG and SVG failed, use default dimensions
                            addImageWithDefaultDimensions(slide, svgFile, slideWidth, slideHeight);
                            continue;
                        }
                        
                        XSLFPictureData pictureData = ppt.addPicture(svgBytes, pictureType);
                        slide.createPicture(pictureData).setAnchor(new Rectangle(xPosition, yPosition, scaledWidth, scaledHeight));
                        System.out.println("Successfully added SVG image to slide " + (i+1) + " as fallback");
                    } catch (Exception ex) {
                        System.err.println("ERROR: Failed to add SVG fallback: " + ex.getMessage());
                        addImageWithDefaultDimensions(slide, svgFile, slideWidth, slideHeight);
                    }
                }
            } catch (Exception e) {
                System.err.println("ERROR: Failed to process slide " + (i+1) + ": " + e.getMessage());
                e.printStackTrace();
                // Try to continue with the next slide
            }
        }
    }

    /**
     * Creates a temporary PNG file from an SVG file for dimension calculation.
     * This is a simple implementation that uses external tools.
     * In a production system, you might want to use a proper SVG renderer like Batik.
     *
     * @param svgFile The SVG file to convert
     * @return A temporary PNG file, or null if conversion failed
     */
    private File createTempPngFromSvg(File svgFile) {
        try {
            // Create a temporary file for the PNG
            Path tempFile = Files.createTempFile("svg_to_png_", ".png");
            File pngFile = tempFile.toFile();
            
            // Use the PlantUML jar to convert SVG to PNG if available
            // This is a simplified approach - you might want to use a dedicated SVG renderer
            ProcessBuilder pb = new ProcessBuilder(
                "java", "-jar", "./plantuml-1.2024.8.jar", 
                "-tpng", svgFile.getAbsolutePath(), 
                "-o", pngFile.getParent()
            );
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0 && pngFile.exists()) {
                return pngFile;
            } else {
                System.err.println("Failed to convert SVG to PNG, exit code: " + exitCode);
                return null;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error converting SVG to PNG: " + e.getMessage());
            return null;
        }
    }

    /**
     * Adds a text slide with bullet points.
     *
     * @param title The title of the slide
     * @param bullets List of bullet points to add
     */
    public void addTextSlide(String title, List<String> bullets) {
        try {
            XSLFSlide slide = ppt.createSlide();
            
            // Add title
            XSLFTextBox titleBox = slide.createTextBox();
            titleBox.setAnchor(new Rectangle(50, 20, ppt.getPageSize().width - 100, 50));
            titleBox.setHorizontalCentered(true);
            
            // Create a text run to set font properties
            XSLFTextRun titleRun = titleBox.addNewTextParagraph().addNewTextRun();
            titleRun.setText(title);
            titleRun.setFontFamily("Bitter"); // Set the font for the title
            titleRun.setFontSize(28.0); // Set the font size

            // Add bullet points
            if (!bullets.isEmpty()) {
                XSLFTextBox bulletBox = slide.createTextBox();
                bulletBox.setAnchor(new Rectangle(50, 80, ppt.getPageSize().width - 100, 
                                                ppt.getPageSize().height - 100));

                for (String bullet : bullets) {
                    XSLFTextParagraph para = bulletBox.addNewTextParagraph();
                    para.setBullet(true); // Enable bullet points
                    para.setBulletFontSize(18.0);
                    XSLFTextRun run = para.addNewTextRun();
                    run.setFontFamily("Inter");
                    run.setText(bullet);
                    run.setFontSize(18.0);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error adding text slide: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a slide with a PNG image directly.
     *
     * @param title     The title for the slide.
     * @param pngFile   The PNG image file to add to the slide.
     */
    public void addImageSlide(String title, File pngFile) {
        try {
            // Get slide dimensions
            int slideWidth = ppt.getPageSize().width;
            int slideHeight = ppt.getPageSize().height;

            // Check if the image file exists
            if (!pngFile.exists()) {
                System.err.println("ERROR: PNG file does not exist: " + pngFile.getAbsolutePath());
                return; // Skip this slide if the image doesn't exist
            }

            // Create a new slide
            XSLFSlide slide = ppt.createSlide();

            // Add the title
            XSLFTextBox titleBox = slide.createTextBox();
            titleBox.setAnchor(new Rectangle(50, 20, slideWidth - 100, 50));
            titleBox.setHorizontalCentered(true);

            // Create a text run to set font properties
            XSLFTextRun titleRun = titleBox.addNewTextParagraph().addNewTextRun();
            titleRun.setText(title);
            titleRun.setFontFamily("Bitter");
            titleRun.setFontSize(26.0);

            // Read the PNG file and get its dimensions
            BufferedImage img = ImageIO.read(pngFile);
            if (img == null) {
                System.err.println("ERROR: Failed to read PNG file: " + pngFile.getAbsolutePath());
                return;
            }

            int imgWidth = img.getWidth();
            int imgHeight = img.getHeight();

            // Calculate the scale factor to fit the image in the slide
            double scaleWidth = (double) (slideWidth - 100) / imgWidth;
            double scaleHeight = (double) (slideHeight - 150) / imgHeight;
            double scale = Math.min(scaleWidth, scaleHeight);

            // Calculate the scaled dimensions
            int scaledWidth = (int) (imgWidth * scale);
            int scaledHeight = (int) (imgHeight * scale);

            // Calculate the position to center the image
            int imgX = (slideWidth - scaledWidth) / 2;
            int imgY = 80 + (slideHeight - 150 - scaledHeight) / 2;

            // Add the image to the slide
            XSLFPictureData pictureData = ppt.addPicture(pngFile, PictureData.PictureType.PNG);
            XSLFPictureShape picture = slide.createPicture(pictureData);
            picture.setAnchor(new Rectangle(imgX, imgY, scaledWidth, scaledHeight));

            System.out.println("Successfully added PNG image to slide: " + title);
        } catch (Exception e) {
            System.err.println("ERROR adding PNG image to slide: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Saves the PowerPoint to the specified file.
     *
     * @param filePath The file path to save the PowerPoint.
     */
    public void save(String filePath) {
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            ppt.write(out);
        } catch (Exception e) {
            throw new RuntimeException("Error saving presentation: " + e.getMessage(), e);
        }
    }
}
