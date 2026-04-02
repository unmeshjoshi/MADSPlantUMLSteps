package com.pumlsteps.presentation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

public class IncludeConfig {
    private String path;
    private String sectionTitle;

    @JsonCreator
    public static IncludeConfig fromJson(JsonNode node) {
        IncludeConfig include = new IncludeConfig();
        if (node == null || node.isNull()) {
            return include;
        }

        if (node.isTextual()) {
            include.setPath(node.asText());
            return include;
        }

        if (node.isObject()) {
            include.setPath(firstNonBlank(node, "path", "file", "include"));
            include.setSectionTitle(firstNonBlank(node, "sectionTitle", "section", "title", "name"));
            return include;
        }

        throw new IllegalArgumentException("Include entries must be a string path or an object with a path.");
    }

    private static String firstNonBlank(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && value.isTextual()) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }
}
