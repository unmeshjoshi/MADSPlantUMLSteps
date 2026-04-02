package com.pumlsteps.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves presentation YAML files, including support for combining multiple
 * YAML files via a top-level {@code includes:} list.
 */
public class PresentationConfigResolver {
    private final ObjectMapper yamlMapper;

    public PresentationConfigResolver() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public PresentationConfig resolve(File yamlFile) throws IOException {
        return resolve(yamlFile, new LinkedHashSet<>());
    }

    private PresentationConfig resolve(File yamlFile, Set<File> resolutionStack) throws IOException {
        File canonicalFile = yamlFile.getCanonicalFile();
        if (!resolutionStack.add(canonicalFile)) {
            throw new IllegalStateException("Circular presentation include detected: " + canonicalFile.getAbsolutePath());
        }

        try {
            PresentationConfig rawConfig = yamlMapper.readValue(canonicalFile, PresentationConfig.class);
            if (rawConfig == null) {
                throw new IllegalStateException("Failed to parse presentation YAML: " + canonicalFile.getAbsolutePath());
            }

            PresentationConfig resolved = new PresentationConfig();
            resolved.setTitle(rawConfig.getTitle());
            resolved.setDescription(rawConfig.getDescription());
            resolved.setIcon(rawConfig.getIcon());
            resolved.setCategory(rawConfig.getCategory());

            List<SectionConfig> orderedSections = new ArrayList<>();

            if (rawConfig.getIncludes() != null) {
                for (String includePath : rawConfig.getIncludes()) {
                    if (includePath == null || includePath.trim().isEmpty()) {
                        continue;
                    }

                    File includedFile = new File(canonicalFile.getParentFile(), includePath).getCanonicalFile();
                    PresentationConfig includedConfig = resolve(includedFile, resolutionStack);
                    adoptMissingMetadata(resolved, includedConfig);
                    orderedSections.addAll(copySections(normalizeToOrderedSections(includedConfig)));
                }
            }

            orderedSections.addAll(copySections(normalizeCurrentConfig(rawConfig)));

            if (!orderedSections.isEmpty()) {
                resolved.setSections(orderedSections);
            }
            resolved.setSlides(null);
            resolved.setIncludes(null);
            return resolved;
        } finally {
            resolutionStack.remove(canonicalFile);
        }
    }

    private void adoptMissingMetadata(PresentationConfig target, PresentationConfig source) {
        if ((target.getTitle() == null || target.getTitle().isBlank()) && source.getTitle() != null && !source.getTitle().isBlank()) {
            target.setTitle(source.getTitle());
        }
        if ((target.getDescription() == null || target.getDescription().isBlank()) && source.getDescription() != null && !source.getDescription().isBlank()) {
            target.setDescription(source.getDescription());
        }
        if ((target.getIcon() == null || target.getIcon().isBlank()) && source.getIcon() != null && !source.getIcon().isBlank()) {
            target.setIcon(source.getIcon());
        }
        if ((target.getCategory() == null || target.getCategory().isBlank()) && source.getCategory() != null && !source.getCategory().isBlank()) {
            target.setCategory(source.getCategory());
        }
    }

    private List<SectionConfig> normalizeCurrentConfig(PresentationConfig config) {
        List<SectionConfig> orderedSections = new ArrayList<>();

        if (config.getSections() != null && !config.getSections().isEmpty()) {
            orderedSections.addAll(copySections(config.getSections()));
        }

        if (config.getSlides() != null && !config.getSlides().isEmpty()) {
            SectionConfig anonymousSection = new SectionConfig();
            anonymousSection.setTitle(null);
            anonymousSection.setSlides(copySlides(config.getSlides()));
            orderedSections.add(anonymousSection);
        }

        return orderedSections;
    }

    private List<SectionConfig> normalizeToOrderedSections(PresentationConfig config) {
        if (config.getSections() != null && !config.getSections().isEmpty()) {
            return config.getSections();
        }

        List<SectionConfig> orderedSections = new ArrayList<>();
        if (config.getSlides() != null && !config.getSlides().isEmpty()) {
            SectionConfig anonymousSection = new SectionConfig();
            anonymousSection.setTitle(null);
            anonymousSection.setSlides(copySlides(config.getSlides()));
            orderedSections.add(anonymousSection);
        }
        return orderedSections;
    }

    private List<SectionConfig> copySections(List<SectionConfig> sections) {
        List<SectionConfig> copies = new ArrayList<>();
        for (SectionConfig section : sections) {
            SectionConfig copy = new SectionConfig();
            copy.setTitle(section.getTitle());
            copy.setSlides(copySlides(section.getSlides()));
            copies.add(copy);
        }
        return copies;
    }

    private List<SlideConfig> copySlides(List<SlideConfig> slides) {
        List<SlideConfig> copies = new ArrayList<>();
        if (slides == null) {
            return copies;
        }

        for (SlideConfig slide : slides) {
            SlideConfig copy = new SlideConfig();
            copy.setTitle(slide.getTitle());
            copy.setType(slide.getType());
            copy.setDiagramRef(slide.getDiagramRef());
            copy.setNotes(slide.getNotes());
            if (slide.getBullets() != null) {
                copy.setBullets(new ArrayList<>(slide.getBullets()));
            }
            copies.add(copy);
        }
        return copies;
    }
}
