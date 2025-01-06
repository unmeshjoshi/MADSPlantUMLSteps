package com.pumlsteps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class PumlFile {
    private static final List<String> IGNORED_FILES = List.of("style.puml");
    private final File file;

    public PumlFile(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
        }
        if (!file.getName().endsWith(".puml")) {
            throw new IllegalArgumentException("Not a PUML file: " + file.getName());
        }
        this.file = file;
    }

    public static List<PumlFile> find(File sourceDir) {
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid source directory: " + sourceDir.getAbsolutePath());
        }

        try (var paths = Files.walk(sourceDir.toPath())) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".puml"))
                    .filter(path -> !IGNORED_FILES.contains(path.getFileName().toString()))
                    .map(path -> new PumlFile(path.toFile()))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Error walking directory: " + sourceDir, e);
        }
    }

    public File createSubDirectory(File outputDir) throws IOException {
        // Simply create a subdirectory using the PUML file's base name
        File targetDir = new File(outputDir, getBaseName());

        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("Failed to create directory: " + targetDir.getAbsolutePath());
        }

        return targetDir;
    }

    public String getBaseName() {
        return file.getName().replace(".puml", "");
    }

    public File getFile() {
        return file;
    }

    public List<String> readLines() throws IOException {
        return Files.readAllLines(file.toPath());
    }

    @Override
    public String toString() {
        return file.getAbsolutePath();
    }
}
