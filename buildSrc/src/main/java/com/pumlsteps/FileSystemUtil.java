package com.pumlsteps;

import java.io.File;
import java.io.IOException;

/**
 * Responsible for file system operations.
 */
public class FileSystemUtil {
    
    /**
     * Checks if a directory exists and creates it if it doesn't.
     *
     * @param directory The directory to check and create
     * @throws IOException If the directory creation fails
     */
    public void checkAndCreateDirectory(File directory) throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Failed to create directory: " + directory.getAbsolutePath());
        }
    }
    
    /**
     * Copies a file from source to destination.
     *
     * @param source The source file
     * @param destination The destination file
     * @throws IOException If the file copy fails
     */
    public void copyFile(File source, File destination) throws IOException {
        if (!source.exists()) {
            throw new IOException("Source file does not exist: " + source.getAbsolutePath());
        }
        
        // Ensure parent directories exist
        File parentDir = destination.getParentFile();
        if (parentDir != null) {
            checkAndCreateDirectory(parentDir);
        }
        
        java.nio.file.Files.copy(
            source.toPath(), 
            destination.toPath(), 
            java.nio.file.StandardCopyOption.REPLACE_EXISTING
        );
    }
    
    /**
     * Lists all subdirectories in a directory.
     *
     * @param directory The directory to list subdirectories from
     * @return An array of subdirectories
     * @throws IOException If the directory does not exist or is not a directory
     */
    public File[] listSubdirectories(File directory) throws IOException {
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IOException("Not a valid directory: " + directory.getAbsolutePath());
        }
        
        return directory.listFiles(File::isDirectory);
    }
    
    /**
     * Creates a temporary directory with the given prefix.
     *
     * @param prefix The prefix for the temporary directory name
     * @return The created temporary directory
     * @throws IOException If the directory creation fails
     */
    public File createTempDirectory(String prefix) throws IOException {
        return java.nio.file.Files.createTempDirectory(prefix).toFile();
    }
    
    /**
     * Deletes a directory and all its contents recursively.
     *
     * @param directory The directory to delete
     * @throws IOException If the directory deletion fails
     */
    public void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        throw new IOException("Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            }
        }
        
        if (!directory.delete()) {
            throw new IOException("Failed to delete directory: " + directory.getAbsolutePath());
        }
    }
}
