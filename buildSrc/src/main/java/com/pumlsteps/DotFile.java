package com.pumlsteps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DotFile {
    private final File file;
    private final String content;

    public DotFile(File file) throws IOException {
        this.file = file;
        this.content = Files.readString(file.toPath());
    }

    public File getFile() {
        return file;
    }

    public String getContent() {
        return content;
    }

    public String getName() {
        return file.getName();
    }

    public Path getPath() {
        return file.toPath();
    }

    @Override
    public String toString() {
        return "DotFile{" +
                "file=" + file +
                ", contentLength=" + content.length() +
                '}';
    }
} 