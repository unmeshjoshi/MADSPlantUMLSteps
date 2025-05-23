package com.pumlsteps.presentation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A simple template loader that handles basic variable substitution
 * and conditional blocks in templates.
 */
public class TemplateLoader {
    private static final Map<String, String> templateCache = new HashMap<>();
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile("\\{\\{#([^}]+)\\}\\}(.*?)\\{\\{/\\1\\}\\}", Pattern.DOTALL);

    /**
     * Load a template from resources and cache it
     */
    public static String loadTemplate(String templateName) throws IOException {
        if (templateCache.containsKey(templateName)) {
            return templateCache.get(templateName);
        }

        try (InputStream is = TemplateLoader.class.getResourceAsStream("/templates/" + templateName)) {
            if (is == null) {
                throw new IOException("Template not found: " + templateName);
            }
            
            String template = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .collect(Collectors.joining("\n"));
            
            templateCache.put(templateName, template);
            return template;
        }
    }

    /**
     * Process a template with the provided data
     */
    public static String processTemplate(String template, Map<String, Object> data) {
        // Process conditional blocks first
        template = processConditionals(template, data);
        
        // Process variables
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = data.get(key);
            String replacement = (value != null) ? value.toString() : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(sb);
        return sb.toString();
    }
    
    /**
     * Process conditional blocks in the template
     */
    private static String processConditionals(String template, Map<String, Object> data) {
        Matcher matcher = CONDITIONAL_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String condition = matcher.group(1);
            String content = matcher.group(2);
            
            Object value = data.get(condition);
            boolean conditionMet = (value != null && (value instanceof Boolean ? (Boolean) value : true));
            
            String replacement = conditionMet ? content : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(sb);
        return sb.toString();
    }
}
