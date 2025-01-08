# PlantUMLSteps

This project automates the process of generating sequence diagrams from `.puml` files using PlantUML. It supports stepwise generation of diagrams, where each step builds upon the previous ones, and includes an interactive viewer and PowerPoint presentation for the generated diagrams.

## Features
- **Stepwise Diagram Generation**: Automatically splits `.puml` files based on step markers with metadata.
- **HTML Viewer**: Generates an `index.html` file for each diagram directory, enabling step-by-step navigation.
- **PowerPoint Generation**: Creates a PowerPoint presentation with all steps.
- **Reusable Style**: Automatically copies a `style.puml` file into the output directories for consistent styling.

## Project Structure
```
project/
├── build.gradle       # Gradle build script
├── src/
│   └── diagrams/         # Source directory for .puml files
│       ├── style.puml    # Reusable style definitions
│       ├── diagram1.puml
│       └── diagram2.puml
├── build/             # Output directory
    └── diagrams/
        ├── diagram1/
        │   ├── step1.puml
        │   ├── step1.png
        │   ├── ...
        │   ├── index.html
        │   └── steps.pptx
        └── diagram2/
            ├── ...
```

## How to Use

### 1. Add Your Diagrams
- Place your `.puml` files in the `src/diagrams` directory.
- Use step markers with JSON metadata to define steps (`'@step1 {"name":"Step Name"}`).
- Use `newPage` property in metadata to start a new diagram.

Example:
```plantuml
@startuml
!include ../style.puml

'@step1 {"name":"Initial Request"}
actor User
User -> System: Hello
'@end1

'@step2 {"name":"System Response", "newPage":true}
System -> User: Hi
'@end2
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
Run the Gradle task:
```bash
./gradlew clean generate
```

This will:
- Generate `.puml` and `.png` files for each step
- Create an `index.html` file in each diagram's output directory for interactive viewing
- Generate a PowerPoint presentation (`steps.pptx`) containing all steps

### 4. View Results
- Open the `build/diagrams/<diagram_name>/index.html` file in a browser to view the step-by-step diagrams
- Open `build/diagrams/<diagram_name>/steps.pptx` to view the PowerPoint presentation

## Customization

### Style
- Define your custom styles in `style.puml` and place it in the `src/diagrams` directory.
- Include it in your diagrams using `!include ../style.puml`

### HTML Viewer
The generated HTML viewer includes:
- Navigation buttons for steps
- Step names display
- Responsive design for different screen sizes

### PowerPoint Output
The generated PowerPoint includes:
- One slide per step
- Step names as slide titles
- Consistent formatting across slides

