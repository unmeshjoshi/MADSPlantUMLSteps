# PlantUMLSteps

This project automates the process of generating sequence diagrams from `.puml` files using PlantUML. It supports stepwise generation of diagrams, where each step builds upon the previous ones, and includes an interactive viewer and PowerPoint presentation for the generated diagrams.

## Features
- **Stepwise Diagram Generation**: Automatically splits `.puml` files based on step markers with metadata.
- **HTML Viewer**: Generates an `index.html` file for each diagram directory, enabling step-by-step navigation.
- **HTML Presentations**: Creates HTML presentations from YAML configuration files.
- **PowerPoint Generation** *(In Progress)*: PowerPoint presentation generation is currently under development and not fully functional.
- **Reusable Style**: Automatically copies a `style.puml` file into the output directories for consistent styling.
- **Global Index**: Generates a main index.html that links to all diagram directories.

## Project Structure
```
project/
├── build.gradle       # Gradle build script with multiple tasks
├── src/
│   ├── diagrams/      # Source directory for .puml files
│   │   ├── style.puml # Reusable style definitions
│   │   ├── diagram1.puml
│   │   └── diagram2.puml
│   └── presentation/  # Presentation configuration files
│       └── slides.yaml
├── build/             # Output directory
    └── diagrams/
        ├── all_steps.pptx  # Global PowerPoint with all diagrams
        ├── index.html      # Global index page
        ├── style.puml      # Copied style file
        ├── diagram1/
        │   ├── step1.puml
        │   ├── step1.svg
        │   ├── ...
        │   └── index.html  # Diagram-specific viewer
        └── diagram2/
            ├── ...
```

## How to Use

### 1. Add Your Diagrams
- Place your `.puml` files in the `src/diagrams` directory.
- Use step markers with JSON metadata to define steps (`' [step1 {"name":"Step Name"}]`).
- Use `newPage` property in metadata to start a new diagram.

Example:
```plantuml
@startuml
!include ../style.puml

' [step1 {"name":"Initial Request"}]
actor User
User -> System: Hello
' [/step1]

' [step2 {"name":"System Response", "newPage":true}]
System -> User: Hi
' [/step2]
@enduml
```

Each step requires:
- `name`: Step name (used in HTML and PowerPoint)
- `newPage`: Optional property to start a new diagram (boolean)

### 2. Add Style File
Create a `style.puml` file in the `src/diagrams` directory with your PlantUML styling:

```plantuml
' src/diagrams/style.puml
skinparam sequence {
    ArrowColor DeepSkyBlue
    ActorBorderColor DeepSkyBlue
    LifeLineBorderColor blue
    LifeLineBackgroundColor #A9DCDF
    
    ParticipantBorderColor DeepSkyBlue
    ParticipantBackgroundColor DodgerBlue
    ParticipantFontName Impact
    ParticipantFontSize 17
    ParticipantFontColor #A9DCDF
}
```

### 3. Generate Diagrams and Presentations
The project provides several Gradle tasks:

```bash
# Generate SVG diagrams
./gradlew svg

# Generate PNG diagrams
./gradlew png

# Generate HTML presentations from YAML configs
./gradlew html

# Generate PowerPoint presentations from YAML configs
./gradlew ppt

# Generate all formats (SVG, PNG, HTML, and PowerPoint)
./gradlew all
```

The available tasks are:

- `svg`: Generates SVG diagrams from PlantUML files
  - Creates `.puml` and `.svg` files for each step
  - Copies the `style.puml` file to the output directory

- `png`: Generates PNG versions of the diagrams
  - Useful for PowerPoint presentations where SVG support may be limited

- `html`: Generates HTML presentations from YAML configuration files
  - Processes YAML files in the `src/presentation` directory
  - Creates interactive HTML presentations with navigation controls
  - Includes a diagram viewer with step-by-step navigation
  - Outputs HTML files to the `build/presentations` directory

- `ppt`: Generates PowerPoint presentations from YAML configuration files
  - Creates professional slides with consistent styling
  - Includes diagram steps with proper titles and numbering
  - Organized by sections for better navigation

- `all`: Runs all the above tasks in the correct sequence

### 4. View Results
- For SVG diagrams, navigate to the `build/diagrams` directory and explore the folders
- For HTML presentations, open the files generated in the `build/presentations` directory
- For PowerPoint presentations, check the `.pptx` files in the `build/presentations` directory

## Customization

### Style
- Define your custom styles in `style.puml` and place it in the `src/diagrams` directory.
- Include it in your diagrams using `!include ../style.puml`

### Presentation Configuration
- The `src/presentation` directory contains YAML files for configuring presentations
- These files are used by the `generateHtml` task

Example of a presentation YAML file:

```yaml
title: "Understanding Consensus"
slides:
  - type: "text"
    title: "Introduction to Consensus"
    bullets:
      - "Consensus is fundamental to distributed systems"
      - "Key challenges in achieving agreement"
      - "Understanding the need for two-phase protocols"
    notes: "Emphasize that consensus is not just about agreement, but about maintaining consistency."
  
  - type: "diagram"
    title: "Single Server Lacks Fault Tolerance"
    diagramRef: "single_server_problem"
    bullets:
      - "Consider a single server. It cannot handle failures."
```

You can also use sections to organize slides:

```yaml
title: "Understanding Kubernetes Architecture"
sections:
  - title: "Introduction"
    slides:
      - type: "text"
        title: "Introduction to Kubernetes Architecture"
        bullets:
          - "Kubernetes is a container orchestration platform"
          - "Key components include Control Plane and Worker Nodes"
      
      - type: "diagram"
        title: "Kubernetes Components"
        diagramRef: "kubernetes_scheduler"
```

### HTML Viewer
The generated HTML viewer includes:
- Navigation buttons for steps
- Step names display
- Responsive design for different screen sizes

### HTML Presentations
The HTML presentations generated from YAML configurations include:
- Structured content with sections and slides
- Support for text and diagram slides
- Speaker notes
- Navigation controls

### PowerPoint Presentations
The PowerPoint presentations include professional features:
- **Professional Design**: Consistent styling with modern color scheme and typography
- **Enhanced Slide Structure**:
  - Section divider slides with accent styling
  - Text slides with proper bullet formatting and adaptive font sizing
  - Image slides with optimized layout and framing
- **Smart Step Numbers**: Shows step numbers in titles for multi-step diagrams, but omits them for single-step diagrams
- **Visual Hierarchy**: Properly formatted titles, bullets, and content with appropriate spacing
- **Navigation Aids**: Includes slide numbers and consistent styling throughout the presentation
- **Optimized Images**: Properly scaled and positioned diagrams with professional framing

