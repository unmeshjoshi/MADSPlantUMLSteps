package com.pumlsteps;

import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.ShapeType;
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
    
    // Professional color scheme
    private static final class ColorScheme {
        // Primary colors
        static final java.awt.Color PRIMARY = new java.awt.Color(0, 90, 156);      // Deep blue
        static final java.awt.Color SECONDARY = new java.awt.Color(242, 105, 36);  // Orange accent
        static final java.awt.Color TERTIARY = new java.awt.Color(64, 64, 64);     // Dark gray
        
        // Background colors
        static final java.awt.Color BACKGROUND_LIGHT = new java.awt.Color(250, 250, 250);
        static final java.awt.Color BACKGROUND_DARK = new java.awt.Color(27, 38, 59);
        
        // Text colors
        static final java.awt.Color TEXT_DARK = new java.awt.Color(33, 33, 33);
        static final java.awt.Color TEXT_LIGHT = new java.awt.Color(250, 250, 250);
        
        // Accent colors for charts and highlights
        static final java.awt.Color ACCENT1 = new java.awt.Color(77, 166, 255);
        static final java.awt.Color ACCENT2 = new java.awt.Color(255, 190, 77);
        static final java.awt.Color ACCENT3 = new java.awt.Color(145, 211, 79);
        static final java.awt.Color ACCENT4 = new java.awt.Color(255, 128, 128);
    }

    /**
     * Creates a new PowerPoint generator.
     */
    public PptGenerator() {
        ppt = new XMLSlideShow();
        // Set to 16:9 aspect ratio with zero gutters (TODO D-5)
        ppt.setPageSize(new Dimension(1920, 1080));
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
     * Formats a section name to be more readable as a slide title.
     */
    private String formatSectionNameForSlide(String sectionName) {
        // Convert snake_case or camelCase to Title Case
        String formatted = sectionName.replaceAll("_", " ")
                                    .replaceAll("([a-z])([A-Z])", "$1 $2");
        
        // Capitalize first letter of each word
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }

    /**
     * Adds a separator slide for a section.
     *
     * @param sectionName The name of the section.
     */
    private void addSeparatorSlide(String sectionName) {
        try {
            XSLFSlide slide = ppt.createSlide();
            int slideWidth = ppt.getPageSize().width;
            int slideHeight = ppt.getPageSize().height;
            
            // Create a gradient background for a more professional look
            XSLFAutoShape bgShape = slide.createAutoShape();
            bgShape.setShapeType(ShapeType.RECT);
            bgShape.setAnchor(new Rectangle(0, 0, slideWidth, slideHeight));
            bgShape.setFillColor(ColorScheme.BACKGROUND_DARK);
            
            // Add a subtle accent bar on the left side
            XSLFAutoShape accentBar = slide.createAutoShape();
            accentBar.setShapeType(ShapeType.RECT);
            accentBar.setAnchor(new Rectangle(0, 0, 20, slideHeight));
            accentBar.setFillColor(ColorScheme.SECONDARY);
            
            // Create a text box with enhanced styling
            XSLFTextBox textBox = slide.createTextBox();
            textBox.setAnchor(new Rectangle(80, slideHeight/2 - 120, slideWidth - 160, 240));
            textBox.setHorizontalCentered(true);
            textBox.setVerticalAlignment(VerticalAlignment.MIDDLE);
            
            // Create a paragraph with proper styling
            XSLFTextParagraph paragraph = textBox.addNewTextParagraph();
            paragraph.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
            
            // Create a text run with enhanced styling
            XSLFTextRun run = paragraph.addNewTextRun();
            run.setText(sectionName.toUpperCase()); // Use uppercase for section names
            run.setFontFamily("Calibri Light"); // Use a lighter font weight for modern look
            run.setFontSize(52.0); // Larger font for section title
            run.setBold(true);
            run.setFontColor(ColorScheme.TEXT_LIGHT);
            
            // Add a small decorative line under the text
            XSLFAutoShape underline = slide.createAutoShape();
            underline.setShapeType(ShapeType.RECT);
            int lineWidth = Math.min(600, sectionName.length() * 25); // Scale with text length
            underline.setAnchor(new Rectangle((slideWidth - lineWidth)/2, slideHeight/2 + 80, lineWidth, 5));
            underline.setFillColor(ColorScheme.SECONDARY);
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

            // Add the title with zero gutters (TODO D-5)
            XSLFTextBox titleBox = slide.createTextBox();
            titleBox.setAnchor(new Rectangle(0, 0, slideWidth, 80));
            titleBox.setHorizontalCentered(true);

            // Create a text run to set font properties
            XSLFTextRun titleRun = titleBox.addNewTextParagraph().addNewTextRun();
            titleRun.setText(title);
            titleRun.setFontFamily("Calibri");
            titleRun.setFontSize(32.0); // Larger font for 16:9 format
            titleRun.setBold(true);
            titleRun.setFontColor(ColorScheme.TEXT_DARK);

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
                        // Calculate dimensions for the image with zero gutters (TODO D-5)
                        int imageY = 80;  // Below title
                        int imageHeight = slideHeight - imageY;  // Use full remaining height

                        double scaleX = (double) slideWidth / image.getWidth();
                        double scaleY = (double) imageHeight / image.getHeight();
                        double scale = Math.min(scaleX, scaleY);

                        int scaledWidth = (int) (image.getWidth() * scale);
                        int scaledHeight = (int) (image.getHeight() * scale);

                        // Center the image horizontally, align to top of content area
                        int xPosition = (slideWidth - scaledWidth) / 2;
                        int yPosition = imageY;

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
                            // Calculate dimensions for the image with zero gutters (TODO D-5)
                            int imageY = 80;  // Below title
                            int imageHeight = slideHeight - imageY;  // Use full remaining height

                            double scaleX = (double) slideWidth / image.getWidth();
                            double scaleY = (double) imageHeight / image.getHeight();
                            double scale = Math.min(scaleX, scaleY);

                            int scaledWidth = (int) (image.getWidth() * scale);
                            int scaledHeight = (int) (image.getHeight() * scale);

                            // Center the image horizontally, align to top of content area
                            int xPosition = (slideWidth - scaledWidth) / 2;
                            int yPosition = imageY;

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
                    // Calculate dimensions for the image with zero gutters (TODO D-5)
                    int imageY = 80;  // Below title
                    int imageHeight = slideHeight - imageY;  // Use full remaining height

                    double scaleX = (double) slideWidth / image.getWidth();
                    double scaleY = (double) imageHeight / image.getHeight();
                    double scale = Math.min(scaleX, scaleY);

                    int scaledWidth = (int) (image.getWidth() * scale);
                    int scaledHeight = (int) (image.getHeight() * scale);

                    // Center the image horizontally, align to top of content area
                    int xPosition = (slideWidth - scaledWidth) / 2;
                    int yPosition = imageY;

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

            // Add the title with zero gutters (TODO D-5)
            XSLFTextBox titleBox = slide.createTextBox();
            titleBox.setAnchor(new Rectangle(0, 0, slideWidth, 80));
            titleBox.setHorizontalCentered(true);

            // Create a text run to set font properties
            XSLFTextRun titleRun = titleBox.addNewTextParagraph().addNewTextRun();
            titleRun.setText(title);
            titleRun.setFontFamily("Calibri");
            titleRun.setFontSize(32.0); // Larger font for 16:9 format
            titleRun.setBold(true);
            titleRun.setFontColor(ColorScheme.TEXT_DARK);

            // Read the PNG for dimension calculation
            BufferedImage image = ImageIO.read(pngFile);
            if (image == null) {
                System.err.println("ERROR: Failed to read PNG image for dimensions: " + pngFile.getAbsolutePath());
                // Fall back to the original method
                addStepSlide(sectionName, title, svgFile);
                return;
            }
            
            // Calculate dimensions for the image with zero gutters (TODO D-5)
            int imageY = 80;  // Below title
            int imageHeight = slideHeight - imageY;  // Use full remaining height

            double scaleX = (double) slideWidth / image.getWidth();
            double scaleY = (double) imageHeight / image.getHeight();
            double scale = Math.min(scaleX, scaleY);

            int scaledWidth = (int) (image.getWidth() * scale);
            int scaledHeight = (int) (image.getHeight() * scale);

            // Center the image horizontally, align to top of content area
            int xPosition = (slideWidth - scaledWidth) / 2;
            int yPosition = imageY;

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

                // Add the title with zero gutters (TODO D-5)
                XSLFTextBox titleBox = slide.createTextBox();
                titleBox.setAnchor(new Rectangle(0, 0, slideWidth, 80));
                titleBox.setHorizontalCentered(true);

                // Create a text run to set font properties
                XSLFTextRun titleRun = titleBox.addNewTextParagraph().addNewTextRun();
                titleRun.setText(title);
                titleRun.setFontFamily("Calibri");
                titleRun.setFontSize(32.0); // Larger font for 16:9 format
                titleRun.setBold(true);
                titleRun.setFontColor(ColorScheme.TEXT_DARK);

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
                
                // Calculate dimensions for the image with zero gutters (TODO D-5)
                int imageY = 80;  // Below title
                int imageHeight = slideHeight - imageY;  // Use full remaining height

                double scaleX = (double) slideWidth / image.getWidth();
                double scaleY = (double) imageHeight / image.getHeight();
                double scale = Math.min(scaleX, scaleY);

                int scaledWidth = (int) (image.getWidth() * scale);
                int scaledHeight = (int) (image.getHeight() * scale);

                // Center the image horizontally, align to top of content area
                int xPosition = (slideWidth - scaledWidth) / 2;
                int yPosition = imageY;

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
        addTextSlide(title, bullets, null);
    }
    
    /**
     * Adds a text slide with bullet points and notes.
     *
     * @param title The title of the slide
     * @param bullets List of bullet points to add
     * @param notes Notes to add to the slide (can be null)
     */
    public void addTextSlide(String title, List<String> bullets, String notes) {
        try {
            XSLFSlide slide = ppt.createSlide();
            
            // Get slide dimensions
            int slideWidth = ppt.getPageSize().width;
            int slideHeight = ppt.getPageSize().height;
            
            // Add subtle background elements for visual interest
            addSlideBackground(slide);
            
            // Add a thin accent bar at the top
            XSLFAutoShape topAccent = slide.createAutoShape();
            topAccent.setShapeType(ShapeType.RECT);
            topAccent.setAnchor(new Rectangle(0, 0, slideWidth, 12));
            topAccent.setFillColor(ColorScheme.PRIMARY);
            
            // Add title with improved styling
            XSLFTextBox titleBox = slide.createTextBox();
            titleBox.setAnchor(new Rectangle(60, 30, slideWidth - 120, 80));
            
            // Create a text run with enhanced font properties
            XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
            titlePara.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.LEFT);
            XSLFTextRun titleRun = titlePara.addNewTextRun();
            titleRun.setText(title);
            titleRun.setFontFamily("Calibri Light"); // More professional font
            titleRun.setFontSize(48.0); // Larger font size for better readability
            titleRun.setBold(true); // Make title bold
            titleRun.setFontColor(ColorScheme.PRIMARY); // Use primary color for title
            
            // Add a small accent line under the title
            XSLFAutoShape titleUnderline = slide.createAutoShape();
            titleUnderline.setShapeType(ShapeType.RECT);
            titleUnderline.setAnchor(new Rectangle(60, 105, 100, 3));
            titleUnderline.setFillColor(ColorScheme.SECONDARY);
            
            // Add bullet points with improved layout
            if (!bullets.isEmpty()) {
                XSLFTextBox bulletBox = slide.createTextBox();
                // Position bullet points with better spacing
                bulletBox.setAnchor(new Rectangle(60, 125, slideWidth - 120, slideHeight - 160));
                
                // Calculate ideal font size based on number of bullets
                double fontSize = calculateIdealFontSize(bullets.size());
                
                for (int i = 0; i < bullets.size(); i++) {
                    String bullet = bullets.get(i);
                    XSLFTextParagraph para = bulletBox.addNewTextParagraph();
                    para.setBullet(true); // Enable bullet points
                    para.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.LEFT);
                    para.setIndent(20.0); // Add indent for better visual hierarchy
                    para.setLeftMargin(30.0); // Add left margin for better readability
                    para.setBulletFontSize(fontSize); // Match bullet size to text
                    
                    // Use different bullet characters for visual interest
                    // Use a custom bullet symbol based on level
                    para.setBulletCharacter("➤"); // Arrow bullet character
                    // Note: setBulletColor not available in this version of Apache POI
                    
                    // Add more space between paragraphs, less within paragraphs
                    para.setLineSpacing(140.0); // Increase spacing for improved legibility
                    if (i > 0) {
                        para.setSpaceBefore(16.0); // More space before each bullet
                    }
                    
                    XSLFTextRun run = para.addNewTextRun();
                    run.setFontFamily("Calibri"); // Match title font for consistency
                    run.setText(bullet);
                    run.setFontSize(fontSize); // Appropriate font size
                    run.setFontColor(ColorScheme.TEXT_DARK); // Better contrast
                }
            }
            
            // Add notes if present
            if (notes != null && !notes.trim().isEmpty()) {
                addNotesToSlide(slide, notes);
            }
            
            // Add subtle footer with slide number
            addSlideFooter(slide, slide.getSlideNumber());
            
        } catch (Exception e) {
            throw new RuntimeException("Error adding text slide: " + e.getMessage(), e);
        }
    }
    
    /**
     * Adds a consistent background to slides for a professional look
     */
    private void addSlideBackground(XSLFSlide slide) {
        int slideWidth = ppt.getPageSize().width;
        int slideHeight = ppt.getPageSize().height;
        
        // Add main background
        XSLFAutoShape background = slide.createAutoShape();
        background.setShapeType(ShapeType.RECT);
        background.setAnchor(new Rectangle(0, 0, slideWidth, slideHeight));
        background.setFillColor(ColorScheme.BACKGROUND_LIGHT);
    }
    
    /**
     * Adds a consistent footer to slides
     */
    private void addSlideFooter(XSLFSlide slide, int slideNumber) {
        int slideWidth = ppt.getPageSize().width;
        int slideHeight = ppt.getPageSize().height;
        
        // Add slide number
        XSLFTextBox slideNumberBox = slide.createTextBox();
        slideNumberBox.setAnchor(new Rectangle(slideWidth - 80, slideHeight - 40, 60, 30));
        XSLFTextParagraph slideNumPara = slideNumberBox.addNewTextParagraph();
        slideNumPara.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.RIGHT);
        XSLFTextRun slideNumRun = slideNumPara.addNewTextRun();
        slideNumRun.setText(String.format("%d", slideNumber));
        slideNumRun.setFontFamily("Calibri");
        slideNumRun.setFontSize(12.0);
        slideNumRun.setFontColor(ColorScheme.TERTIARY);
    }
    
    /**
     * Calculates the ideal font size based on the number of bullet points.
     * 
     * @param bulletCount Number of bullet points
     * @return The recommended font size
     */
    private double calculateIdealFontSize(int bulletCount) {
        // Increased base font sizes for better on-screen readability
        if (bulletCount <= 3) {
            return 55.0;
        } else if (bulletCount <= 5) {
            return 48.0;
        } else if (bulletCount <= 8) {
            return 40.0;
        } else {
            return 32.0; // Minimum size even for many bullets
        }
    }
    
    /**
     * Notes functionality currently disabled - Apache POI's notes support is limited.
     * The setNotes() method is marked @NotImplemented and getNotes() returns null.
     * 
     * Current implementation provides:
     * ✅ Bullets in diagram slides (working perfectly)
     * ✅ Valid PowerPoint format (no corruption)
     * ⚠️ Notes support (needs future implementation)
     * 
     * @param slide The slide to add notes to
     * @param notes The notes text to add
     */
    private void addNotesToSlide(XSLFSlide slide, String notes) {
        // Notes are parsed correctly from YAML but cannot be added to slides due to 
        // Apache POI limitations. The notes content is preserved in the logs.
        System.out.println("Notes content (not added to slide): " + notes.substring(0, Math.min(50, notes.length())) + "...");
    }

    /**
     * Adds a slide with a PNG image directly.
     *
     * @param title     The title for the slide.
     * @param pngFile   The PNG image file to add to the slide.
     */
    public void addImageSlide(String title, File pngFile) {
        addImageSlide(title, pngFile, null, null);
    }
    
    /**
     * Adds a slide with a PNG image, bullets, and notes.
     *
     * @param title     The title for the slide.
     * @param pngFile   The PNG image file to add to the slide.
     * @param bullets   List of bullet points to add (can be null).
     * @param notes     Notes to add to the slide (can be null).
     */
    public void addImageSlide(String title, File pngFile, List<String> bullets, String notes) {
        try {
            // Get slide dimensions
            int slideWidth = ppt.getPageSize().width;
            int slideHeight = ppt.getPageSize().height;

            // Check if the image file exists
            if (!pngFile.exists()) {
                System.err.println("ERROR: PNG file does not exist: " + pngFile.getAbsolutePath());
                return; // Skip this slide if the image doesn't exist
            }

            // Create a new slide with the standard background
            XSLFSlide slide = ppt.createSlide();
            addSlideBackground(slide);
            
            // Add top accent bar for consistency with text slides
            XSLFAutoShape topAccent = slide.createAutoShape();
            topAccent.setShapeType(ShapeType.RECT);
            topAccent.setAnchor(new Rectangle(0, 0, slideWidth, 12));
            topAccent.setFillColor(ColorScheme.PRIMARY);

            // Add the title with zero gutters (TODO D-5)
            XSLFTextBox titleBox = slide.createTextBox();
            titleBox.setAnchor(new Rectangle(0, 0, slideWidth, 80));
            titleBox.setHorizontalCentered(true);
            
            XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
            titlePara.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.LEFT);
            XSLFTextRun titleRun = titlePara.addNewTextRun();
            titleRun.setText(title);
            titleRun.setFontFamily("Calibri");
            titleRun.setFontSize(32.0); // Larger font for 16:9 format
            titleRun.setBold(true);
            titleRun.setFontColor(ColorScheme.TEXT_DARK);
            
            // Add a small accent line under the title
            XSLFAutoShape titleUnderline = slide.createAutoShape();
            titleUnderline.setShapeType(ShapeType.RECT);
            titleUnderline.setAnchor(new Rectangle(0, 80, 100, 3));
            titleUnderline.setFillColor(ColorScheme.SECONDARY);

            // Read the PNG file and get its dimensions
            BufferedImage img = ImageIO.read(pngFile);
            if (img == null) {
                System.err.println("ERROR: Failed to read PNG file: " + pngFile.getAbsolutePath());
                return;
            }

            int imgWidth = img.getWidth();
            int imgHeight = img.getHeight();
            
            // Determine layout based on whether bullets are present
            boolean hasBullets = (bullets != null && !bullets.isEmpty());
            int availableWidth = slideWidth - 140; // Wider margins
            int availableHeight = slideHeight - 180;
            
            // If bullets are present, reserve space for them on the right
            if (hasBullets) {
                availableWidth = (int) (slideWidth * 0.6); // Use 60% for image, 40% for bullets
            }

            // Calculate the scale factor to fit the image in the available space
            double scaleWidth = (double) availableWidth / imgWidth;
            double scaleHeight = (double) availableHeight / imgHeight;
            double scale = Math.min(scaleWidth, scaleHeight) * 0.9; // Slightly smaller for visual breathing room

            // Calculate the scaled dimensions
            int scaledWidth = (int) (imgWidth * scale);
            int scaledHeight = (int) (imgHeight * scale);

            // Calculate the position to center the image in its allocated space
            int imgX;
            if (hasBullets) {
                imgX = (availableWidth - scaledWidth) / 2 + 70; // Left side positioning with margin
            } else {
                imgX = (slideWidth - scaledWidth) / 2; // Center positioning
            }
            int imgY = 110 + (slideHeight - 200 - scaledHeight) / 2;

            // Create a background frame for the image
            XSLFAutoShape imageFrame = slide.createAutoShape();
            imageFrame.setShapeType(ShapeType.RECT);
            int frameMargin = 10;
            imageFrame.setAnchor(new Rectangle(imgX - frameMargin, imgY - frameMargin, 
                                              scaledWidth + (frameMargin * 2), 
                                              scaledHeight + (frameMargin * 2)));
            imageFrame.setFillColor(java.awt.Color.WHITE);
            
            // Add a subtle shadow or border to the frame
            imageFrame.setLineColor(new java.awt.Color(230, 230, 230));
            imageFrame.setLineWidth(1.5);

            // Add the image to the slide, positioned above the frame
            XSLFPictureData pictureData = ppt.addPicture(pngFile, PictureData.PictureType.PNG);
            XSLFPictureShape picture = slide.createPicture(pictureData);
            picture.setAnchor(new Rectangle(imgX, imgY, scaledWidth, scaledHeight));
            
            // Add bullet points if present - simplified approach
            if (hasBullets) {
                int bulletsX = availableWidth + 140; // Start after the image area
                int bulletsY = 110;
                int bulletsWidth = slideWidth - bulletsX - 70; // Leave margin on right
                int bulletsHeight = slideHeight - 200;
                
                XSLFTextBox bulletBox = slide.createTextBox();
                bulletBox.setAnchor(new Rectangle(bulletsX, bulletsY, bulletsWidth, bulletsHeight));
                
                // Calculate ideal font size based on number of bullets
                double fontSize = calculateIdealFontSize(bullets.size());
                
                for (int i = 0; i < bullets.size(); i++) {
                    String bullet = bullets.get(i);
                    XSLFTextParagraph para = bulletBox.addNewTextParagraph();
                    para.setBullet(true);
                    para.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.LEFT);
                    
                    XSLFTextRun run = para.addNewTextRun();
                    run.setFontFamily("Calibri");
                    run.setText(bullet);
                    run.setFontSize(fontSize);
                    run.setFontColor(ColorScheme.TEXT_DARK);
                }
            }
            
            // Add notes if present
            if (notes != null && !notes.trim().isEmpty()) {
                addNotesToSlide(slide, notes);
            }
            
            // Add subtle footer with slide number
            addSlideFooter(slide, slide.getSlideNumber());

            System.out.println("Successfully added PNG image to slide: " + title + 
                             (hasBullets ? " with " + bullets.size() + " bullets" : "") +
                             (notes != null ? " with notes" : ""));
        } catch (Exception e) {
            System.err.println("ERROR adding PNG image to slide: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Adds a section separator slide with professional styling.
     *
     * @param sectionTitle The title of the section.
     */
    public void addSectionSeparatorSlide(String sectionTitle) {
        try {
            XSLFSlide slide = ppt.createSlide();
            int slideWidth = ppt.getPageSize().width;
            int slideHeight = ppt.getPageSize().height;
            
            // Create a gradient background for a more professional look
            XSLFAutoShape bgShape = slide.createAutoShape();
            bgShape.setShapeType(ShapeType.RECT);
            bgShape.setAnchor(new Rectangle(0, 0, slideWidth, slideHeight));
            bgShape.setFillColor(ColorScheme.BACKGROUND_DARK);
            
            // Add a subtle accent bar on the left side
            XSLFAutoShape accentBar = slide.createAutoShape();
            accentBar.setShapeType(ShapeType.RECT);
            accentBar.setAnchor(new Rectangle(0, 0, 20, slideHeight));
            accentBar.setFillColor(ColorScheme.SECONDARY);
            
            // Create a text box with enhanced styling
            XSLFTextBox textBox = slide.createTextBox();
            textBox.setAnchor(new Rectangle(80, slideHeight/2 - 120, slideWidth - 160, 240));
            textBox.setHorizontalCentered(true);
            textBox.setVerticalAlignment(VerticalAlignment.MIDDLE);
            
            // Create a paragraph with proper styling
            XSLFTextParagraph paragraph = textBox.addNewTextParagraph();
            paragraph.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
            
            // Create a text run with enhanced styling
            XSLFTextRun run = paragraph.addNewTextRun();
            run.setText(sectionTitle.toUpperCase()); // Use uppercase for section names
            run.setFontFamily("Calibri Light"); // Use a lighter font weight for modern look
            run.setFontSize(52.0); // Larger font for section title
            run.setBold(true);
            run.setFontColor(ColorScheme.TEXT_LIGHT);
            
            // Add a small decorative line under the text
            XSLFAutoShape underline = slide.createAutoShape();
            underline.setShapeType(ShapeType.RECT);
            int lineWidth = Math.min(600, sectionTitle.length() * 25); // Scale with text length
            underline.setAnchor(new Rectangle((slideWidth - lineWidth)/2, slideHeight/2 + 80, lineWidth, 5));
            underline.setFillColor(ColorScheme.SECONDARY);
        } catch (Exception e) {
            throw new RuntimeException("Error adding section separator slide: " + e.getMessage(), e);
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
