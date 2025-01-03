package com.pumlsteps;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StepParserTest {
    private StepParser parser;

    @BeforeEach
    void setUp() {
        parser = new StepParser();
    }

    @Test
    void testIsStepStart_ValidStartLine() {
        String line = "' [step10 {name:\"Initialize the System\"}]";
        assertTrue(parser.isStepStart(line));
    }

    @Test
    void testIsStepStart_InvalidStartLine() {
        String line = "' [step {name:\"Initialize the System\"}]"; // Missing step number
        assertFalse(parser.isStepStart(line));
    }

    @Test
    void testIsStepEnd_ValidEndLine() {
        String line = "' [/step10]";
        assertTrue(parser.isStepEnd(line, 10));
    }

    @Test
    void testIsStepEnd_InvalidEndLine_WrongStepNumber() {
        String line = "' [/step20]";
        assertFalse(parser.isStepEnd(line, 10));
    }

    @Test
    void testParseMetadata_ValidMetadata() {
        String line = "' [step10 {\"name\":\"Initialize the System\", \"author\":\"Alice\"}]";
        Map<String, Object> metadata = parser.parseMetadata(line);

        assertNotNull(metadata);
        assertEquals(10, metadata.get("step"));
        assertEquals("Initialize the System", metadata.get("name"));
        assertEquals("Alice", metadata.get("author"));
    }

    @Test
    void testParseMetadata_MissingMetadata() {
        String line = "' [step10 {}]";
        Map<String, Object> metadata = parser.parseMetadata(line);

        assertNotNull(metadata);
        assertEquals(10, metadata.get("step"));
        assertEquals(1, metadata.size()); // Only the step key is present
    }

    @Test
    void testParseMetadata_InvalidJson() {
        String line = "' [step10 {\"name\":\"Initialize the System\", invalidKey}]";
        Exception exception = assertThrows(RuntimeException.class, () -> parser.parseMetadata(line));
        assertTrue(exception.getMessage().contains("Error parsing metadata"));
    }

    @Test
    void testParseMetadata_EmptyMetadata() {
        String line = "' [step10 ]"; // Missing curly braces
        assertNull(parser.parseMetadata(line));
    }

    @Test
    void testSimpleStepWithoutMetadata() throws Exception {
        File tempFile = createTempFile("""
        @startuml
           ' [step1]
            group #LightYellow "Controller Initialization" 
            b1 -> b1: Initialize 
            ' [/step1]
            
            ' [step2]
            b1 -> zk: Load broker metadata
            
            ' [/step2]
            
            ' [step3]
            b1 -> zk: Subscribe to topic changes
            deactivate b1
            end
            ' [/step3]
        @enduml
    """);

        ParsedPlantUmlFile parsedPlantUmlFile = parser.parse(tempFile);

        // Assert file name
        assertEquals(getNameWithoutExtension(tempFile), parsedPlantUmlFile.getName());

        // Assert step count
        List<Step> steps = parsedPlantUmlFile.getSteps();
        assertEquals(3, steps.size());


    }

    @Test
    void testParseSingleStep() throws Exception {
        File tempFile = createTempFile("""
            @startuml
            participant "User" as U
            participant "System" as S

            ' [step1 {"name":"Initialize the System", "author":"Alice"}]
            U -> S: Request initialization
            S -> U: Initialization successful
            ' [/step1]

            @enduml
        """);

        ParsedPlantUmlFile parsedPlantUmlFile = parser.parse(tempFile);

        assertEquals(getNameWithoutExtension(tempFile), parsedPlantUmlFile.getName());
        List<Step> steps = parsedPlantUmlFile.getSteps();
        assertEquals(1, steps.size());

        Step step1 = steps.get(0);
        assertEquals(1, step1.getStepNumber());
        assertEquals("Initialize the System", step1.getMetadata().get("name"));
        assertEquals("Alice", step1.getMetadata().get("author"));
        assertTrue(step1.getContent().contains("Request initialization"));
        assertTrue(step1.getContent().contains("Initialization successful"));
    }

    private static @NotNull String getNameWithoutExtension(File tempFile) {
        return tempFile.getName().substring(0, tempFile.getName().indexOf("."));
    }

    @Test
    void testParseMultipleSteps() throws Exception {
        File tempFile = createTempFile("""
            @startuml
            participant "User" as U
            participant "System" as S

            ' [step1 {"name":"Initialize the System", "author":"Alice"}]
            U -> S: Request initialization
            S -> U: Initialization successful
            ' [/step1]

            ' [step2 {"name":"Process Request", "author":"Bob"}]
            S -> S: Internal processing
            S -> U: Request processed
            ' [/step2]

            @enduml
        """);

        ParsedPlantUmlFile parsedPlantUmlFile = parser.parse(tempFile);

        assertEquals(getNameWithoutExtension(tempFile), parsedPlantUmlFile.getName());
        List<Step> steps = parsedPlantUmlFile.getSteps();
        assertEquals(2, steps.size());

        Step step1 = steps.get(0);
        assertEquals(1, step1.getStepNumber());
        assertEquals("Initialize the System", step1.getMetadata().get("name"));
        assertTrue(step1.getContent().contains("Request initialization"));

        Step step2 = steps.get(1);
        assertEquals(2, step2.getStepNumber());
        assertEquals("Process Request", step2.getMetadata().get("name"));
        assertTrue(step2.getContent().contains("Internal processing"));
    }

    @Test
    void testPreservePreviousStepContent() throws Exception {
        File tempFile = createTempFile("""
            @startuml
            participant "User" as U
            participant "System" as S

            ' [step1 {"name":"Initialize the System", "author":"Alice"}]
            U -> S: Request initialization
            S -> U: Initialization successful
            ' [/step1]

            ' [step2 {"name":"Process Request", "author":"Bob"}]
            S -> S: Internal processing
            S -> U: Request processed
            ' [/step2]

            @enduml
        """);

        ParsedPlantUmlFile parsedPlantUmlFile = parser.parse(tempFile);
        List<Step> steps = parsedPlantUmlFile.getSteps();

        Step step1 = steps.get(0);
        Step step2 = steps.get(1);

        assertTrue(step1.getContent().contains("Request initialization"));
        assertTrue(step2.getContent().contains("Internal processing"));
       assertTrue(step2.getContent().contains("Request initialization")); // Verify separation of content
    }

    @Test
    void testNestedStepsNotAllowed() throws Exception {
        File tempFile = createTempFile("""
            @startuml
            ' [step1 {"name":"Outer Step", "author":"Alice"}]
            participant "User" as U

            ' [step2 {"name":"Inner Step", "author":"Bob"}]
            U -> System: Invalid nesting
            ' [/step2]

            ' [/step1]
            @enduml
        """);

        assertThrows(IllegalStateException.class, () -> parser.parse(tempFile), "Nested steps are not allowed.");
    }

    @Test
    void testMissingEndTag() throws Exception {
        File tempFile = createTempFile("""
            @startuml
            ' [step1 {"name":"Missing End", "author":"Alice"}]
            participant "User" as U
            U -> System: Action without end tag
            @enduml
        """);

        ParsedPlantUmlFile parsedPlantUmlFile = parser.parse(tempFile);
        assertEquals(1, parsedPlantUmlFile.getSteps().size());

        Step step1 = parsedPlantUmlFile.getSteps().get(0);
        assertTrue(step1.getContent().contains("Action without end tag"));
    }

    @Test
    void testValidStepContentForAllScenarios() throws Exception {
        File tempFile = createTempFile("""
        @startuml
        participant "User" as U
        participant "System" as S

        ' [step1 {"name":"Initialize the System", "author":"Alice"}]
        U -> S: Request initialization
        S -> U: Initialization successful
        ' [/step1]

        ' [step2 {"name":"Process Request", "author":"Bob"}]
        S -> S: Internal processing
        S -> U: Request processed
        ' [/step2]
      \s
        ' [step3 {"name":"Process Request Again", "author":"Bob"}]
        S -> S: Send it again
        S -> U: Again processed the request
        ' [/step3]
        @enduml
   \s""");

        ParsedPlantUmlFile parsedPlantUmlFile = parser.parse(tempFile);

        // Assert step count
        List<Step> steps = parsedPlantUmlFile.getSteps();
        assertEquals(3, steps.size());

        // Verify each step content
        for (Step step : steps) {
            String content = step.getContent();
            assertNotNull(content, "Step content should not be null");
            System.out.println("content = " + content);
            assertTrue(content.trim().startsWith("@startuml\n"), "Step content should start with '@startuml'");
            assertTrue(content.trim().endsWith("@enduml"), "Step content should end with '@enduml'");
        }

        // Step-specific assertions
        Step step1 = steps.get(0);
        assertTrue(step1.getContent().contains("Request initialization"));
        assertTrue(step1.getContent().contains("Initialization successful"));

        Step step2 = steps.get(1);
        assertTrue(step2.getContent().contains("Internal processing"));
        assertTrue(step2.getContent().contains("Request processed"));

        Step step3 = steps.get(2);
        assertTrue(step3.getContent().contains("Internal processing"));
        assertTrue(step3.getContent().contains("Request processed"));
        assertTrue(step3.getContent().contains("Again processed the request"));
    }


    private File createTempFile(String content) throws Exception {
        File tempFile = File.createTempFile("temp", ".puml");
        Files.writeString(tempFile.toPath(), content);
        tempFile.deleteOnExit();
        return tempFile;
    }
}
