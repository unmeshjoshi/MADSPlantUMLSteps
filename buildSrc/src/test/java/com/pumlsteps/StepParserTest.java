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
    void testParseMetadata_ValidMetadata() {
        String line = "' [step10 {\"name\":\"Initialize the System\", \"author\":\"Alice\"}]";
        Map<String, Object> metadata = parser.parseMetadata(line);

        assertNotNull(metadata);
        assertEquals("Initialize the System", metadata.get("name"));
        assertEquals("Alice", metadata.get("author"));
    }

    @Test
    void testParseMetadata_MissingMetadata() {
        String line = "' [step10 {}]";
        Map<String, Object> metadata = parser.parseMetadata(line);

        assertNotNull(metadata);
        assertEquals(0, metadata.size()); // Only the step key is present
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
        assertTrue(parser.parseMetadata(line).isEmpty());
    }

    @Test
    void testSimpleStepWithoutMetadata() throws Exception {
        var tempFile = createTempFile("""
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
        assertEquals(tempFile.getBaseName(), parsedPlantUmlFile.getName());

        // Assert step count
        List<Step> steps = parsedPlantUmlFile.getSteps();
        assertEquals(3, steps.size());


    }

    @Test
    void testParseSingleStep() throws Exception {
        var tempFile = createTempFile("""
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

        assertEquals(tempFile.getBaseName(), parsedPlantUmlFile.getName());
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
        var tempFile = createTempFile("""
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

        assertEquals(tempFile.getBaseName(), parsedPlantUmlFile.getName());
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
    void testParseMultipleStepsNotExplicitlyNumbered() throws Exception {
        var tempFile = createTempFile("""
            @startuml
            participant "User" as U
            participant "System" as S

            ' [step {"name":"Initialize the System", "author":"Alice"}]
            U -> S: Request initialization
            S -> U: Initialization successful
         
            ' [step {"name":"Process Request", "author":"Bob"}]
            S -> S: Internal processing
            S -> U: Request processed

            @enduml
        """);

        ParsedPlantUmlFile parsedPlantUmlFile = parser.parse(tempFile);

        assertEquals(tempFile.getBaseName(), parsedPlantUmlFile.getName());
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
        var tempFile = createTempFile("""
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
    void testMissingEndTag() throws Exception {
        var tempFile = createTempFile("""
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
        var tempFile = createTempFile("""
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

    @Test
    void testNewPageStepStartsWithFreshContent() throws Exception {
        var tempFile = createTempFile("""
            @startuml
            participant "User" as U
            participant "System" as S

            ' [step1 {"name":"First Page"}]
            U -> S: First action
            ' [/step1]

            ' [step2 {"name":"Second Page", "newPage":true}]
            S -> U: Second action
            ' [/step2]
            @enduml
        """);

        ParsedPlantUmlFile parsedPlantUmlFile = parser.parse(tempFile);
        List<Step> steps = parsedPlantUmlFile.getSteps();

        Step step2 = steps.get(1);
        assertFalse(step2.getContent().contains("First action"), 
            "New page step should start fresh without previous content");
        assertTrue(step2.getContent().contains("Second action"));
        assertTrue(step2.getContent().startsWith("@startuml"));
    }

    @Test
    public void shouldPreserveIncludesAcrossNewPages() throws Exception {
        // Arrange
        String content = """
            !include ../style.puml
            !include ../common.puml
            @startuml
            ' [step1 {"name":"First Step"}]
            actor User
            User -> System: Request
            '[/step1]
            
            ' [step2 {"name":"Second Step", "newPage":true}]
            actor User
            User -> System: Process
            ' [/step2]
            
            ' [step3 {"name":"Third Step", "newPage":true}]
            actor User
            User -> System: Response
            ' [/step3]
            @enduml
            """;

        PumlFile pumlFile = createTempFile(content);
        StepParser parser = new StepParser();

        // Act
        ParsedPlantUmlFile result = parser.parse(pumlFile);

        // Assert
        List<Step> steps = result.getSteps();
        assertEquals(3, steps.size());

        // First step should have includes
        assertTrue(steps.get(0).getContent().contains("!include ../style.puml"));
        assertTrue(steps.get(0).getContent().contains("!include ../common.puml"));

        // Second step (new page) should have includes
        assertTrue(steps.get(1).getContent().contains("!include ../style.puml"));
        assertTrue(steps.get(1).getContent().contains("!include ../common.puml"));
        assertTrue(steps.get(1).getContent().contains("@startuml"));

        // Third step (new page) should have includes
        assertTrue(steps.get(2).getContent().contains("!include ../style.puml"));
        assertTrue(steps.get(2).getContent().contains("!include ../common.puml"));
        assertTrue(steps.get(2).getContent().contains("@startuml"));
    }


    private PumlFile createTempFile(String content) throws Exception {
        var tempFile = File.createTempFile("temp", ".puml");
        Files.writeString(tempFile.toPath(), content);
        tempFile.deleteOnExit();
        return new PumlFile(tempFile);
    }
}
