async function loadVersionInfo() {
    try {
        const response = await fetch('version.txt');
        if (response.ok) {
            const versionInfo = await response.text();
            const versionDiv = document.createElement('div');
            versionDiv.className = 'version-info';
            versionDiv.textContent = versionInfo;
            document.body.appendChild(versionDiv);
        }
    } catch (error) {
        console.error('Failed to load version info:', error);
    }
}

document.addEventListener("DOMContentLoaded", function () {
    loadVersionInfo();
    // Handle title notes positioning
    document.querySelectorAll('.title-notes').forEach(noteEl => {
        const spanEl = noteEl.querySelector('span');
        noteEl.addEventListener('mouseenter', () => {
            const rect = noteEl.getBoundingClientRect();
            const viewportWidth = window.innerWidth;
            const viewportHeight = window.innerHeight;
            
            // Calculate available space on each side
            const spaceRight = viewportWidth - rect.right;
            const spaceBottom = viewportHeight - rect.bottom;
            
            // Position vertically
            if (spaceBottom < 200) { // If not enough space below
                spanEl.style.top = `${rect.top - 10}px`;
                spanEl.style.transform = 'translateY(-100%)';
            } else {
                spanEl.style.top = `${rect.bottom + 10}px`;
                spanEl.style.transform = 'none';
            }
            
            // Position horizontally
            if (spaceRight < 420) { // If not enough space to the right (400px width + 20px margin)
                spanEl.style.left = 'auto';
                spanEl.style.right = '20px';
            } else {
                spanEl.style.left = `${rect.left}px`;
                spanEl.style.right = 'auto';
            }
        });
    });

    const prevButton = document.getElementById("prev-button");
    const nextButton = document.getElementById("next-button");
    let currentSlideIndex = 0; // Tracks the current slide
    let currentStepIndex = 0; // Tracks the current step within a slide

    const slides = document.querySelectorAll(".slide");

    function updateButtonStates() {
        // Enable/disable previous button based on current position
        prevButton.disabled = currentSlideIndex === 0 && currentStepIndex === 0;

        // Get current slide and its steps
        const currentSlide = slides[currentSlideIndex];
        const steps = currentSlide.querySelectorAll(".step");

        // Enable/disable next button based on current position
        const isLastSlide = currentSlideIndex === slides.length - 1;
        const isLastStep = steps.length > 0 ? currentStepIndex === steps.length - 1 : true;
        nextButton.disabled = isLastSlide && isLastStep;
    }

    function showSlide(index) {
        slides.forEach((slide, i) => {
            slide.style.display = i === index ? "block" : "none";
        });
        currentStepIndex = 0; // Reset step index when changing slides
        showStep(slides[index]);
        updateButtonStates();
    }

    function showStep(slide) {
        const steps = Array.from(slide.querySelectorAll(".step"))
            .sort((a, b) => parseInt(a.getAttribute("step_index")) - parseInt(b.getAttribute("step_index"))); // Sort steps by step_index

        steps.forEach((step, i) => {
            step.style.display = i === currentStepIndex ? "block" : "none";
        });
        updateButtonStates();
    }

    function nextStep() {
        const slide = slides[currentSlideIndex];
        const steps = slide.querySelectorAll(".step");

        if (currentStepIndex < steps.length - 1) {
            currentStepIndex++;
            showStep(slide);
        } else {
            // Move to the next slide if no more steps
            if (currentSlideIndex < slides.length - 1) {
                currentSlideIndex++;
                showSlide(currentSlideIndex);
            }
        }
    }

    function prevStep() {
        const slide = slides[currentSlideIndex];
        const steps = slide.querySelectorAll(".step");

        if (currentStepIndex > 0) {
            currentStepIndex--;
            showStep(slide);
        } else {
            // Move to the previous slide if on the first step
            if (currentSlideIndex > 0) {
                currentSlideIndex--;
                showSlide(currentSlideIndex);

                // Show the last step of the new current slide if it has steps
                const newSlide = slides[currentSlideIndex];
                const newSteps = newSlide.querySelectorAll(".step");
                if (newSteps.length > 0) {
                    currentStepIndex = newSteps.length - 1;
                    showStep(newSlide);
                }
            }
        }
    }

    // Initialize by showing the first slide
    showSlide(currentSlideIndex);

    // Attach the functions to the navigation buttons
    document.getElementById("next-button").addEventListener("click", nextStep);
    document.getElementById("prev-button").addEventListener("click", prevStep);
});