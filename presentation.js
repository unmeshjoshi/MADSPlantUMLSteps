class PresentationViewer {
    constructor() {
        this.currentSlideIndex = 0;
        this.currentStepIndex = 0;
        this.slides = [];
        this.isFullscreen = false;
        this.isDrawingMode = false;
        this.isLaserMode = false;
        this.drawings = [];
        this.currentPath = null;
        this.laserPointer = null;
        
        this.init();
    }

    async init() {
        await this.loadVersionInfo();
        this.setupElements();
        this.setupEventListeners();
        this.setupKeyboardShortcuts();
        this.setupDrawingCanvas();
        this.setupLaserPointer();
        this.setupFullscreenControls();
        this.initializePresentation();
    }

    async loadVersionInfo() {
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

    setupElements() {
        this.slides = document.querySelectorAll(".slide");
        this.prevButton = document.getElementById("prev-button");
        this.nextButton = document.getElementById("next-button");
        this.progressIndicator = document.querySelector('.progress-indicator');
        this.sectionInfo = document.getElementById('current-section');
        
        // Slide selector elements
        this.slideSelectorModal = document.getElementById('slide-selector-modal');
        this.slideList = document.getElementById('slide-list');
        this.slideSearchInput = document.getElementById('slide-search-input');
        this.gotoSlideButton = document.getElementById('goto-slide-button');
        this.closeSlideSelectorButton = document.getElementById('close-slide-selector');
        
        // Create toolbar
        this.createToolbar();
        
        // Handle title notes positioning
        this.setupTitleNotes();
        
        // Initialize slide selector
        this.initializeSlideSelector();
    }

    createToolbar() {
        const toolbar = document.createElement('div');
        toolbar.className = 'presentation-toolbar';
        toolbar.innerHTML = `
            <div class="toolbar-group">
                <button id="fullscreen-btn" class="toolbar-btn" title="Toggle Fullscreen (F)">
                    <i class="fas fa-expand"></i>
                </button>
                <button id="laser-btn" class="toolbar-btn" title="Laser Pointer (L)">
                    <i class="fas fa-dot-circle"></i>
                </button>
                <button id="draw-btn" class="toolbar-btn" title="Drawing Mode (D)">
                    <i class="fas fa-pen"></i>
                </button>
                <button id="clear-btn" class="toolbar-btn" title="Clear Drawings (C)">
                    <i class="fas fa-eraser"></i>
                </button>
            </div>
            <div class="toolbar-group">
                <span class="slide-counter">
                    <span id="current-slide-num">1</span> / <span id="total-slides">${this.slides.length}</span>
                </span>
            </div>
        `;
        
        document.body.appendChild(toolbar);
        
        // Store references
        this.fullscreenBtn = document.getElementById('fullscreen-btn');
        this.laserBtn = document.getElementById('laser-btn');
        this.drawBtn = document.getElementById('draw-btn');
        this.clearBtn = document.getElementById('clear-btn');
        this.currentSlideNum = document.getElementById('current-slide-num');
    }

    setupDrawingCanvas() {
        // Create canvas overlay for drawings
        this.canvas = document.createElement('canvas');
        this.canvas.className = 'drawing-canvas';
        this.canvas.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100vw;
            height: 100vh;
            pointer-events: none;
            z-index: 9999;
            display: none;
        `;
        document.body.appendChild(this.canvas);
        
        this.ctx = this.canvas.getContext('2d');
        this.resizeCanvas();
        
        // Setup drawing properties
        this.ctx.strokeStyle = '#ff4444';
        this.ctx.lineWidth = 3;
        this.ctx.lineCap = 'round';
        this.ctx.lineJoin = 'round';
    }

    setupLaserPointer() {
        this.laserPointer = document.createElement('div');
        this.laserPointer.className = 'laser-pointer';
        this.laserPointer.style.cssText = `
            position: fixed;
            width: 20px;
            height: 20px;
            border-radius: 50%;
            background: radial-gradient(circle, #ff0000 0%, #ff0000 30%, transparent 70%);
            box-shadow: 0 0 20px #ff0000, 0 0 40px #ff0000;
            pointer-events: none;
            z-index: 9998;
            display: none;
            transform: translate(-50%, -50%);
        `;
        document.body.appendChild(this.laserPointer);
    }

    setupFullscreenControls() {
        // Create fullscreen exit button
        this.exitFullscreenBtn = document.createElement('button');
        this.exitFullscreenBtn.className = 'exit-fullscreen-btn';
        this.exitFullscreenBtn.innerHTML = '<i class="fas fa-times"></i>';
        this.exitFullscreenBtn.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            width: 40px;
            height: 40px;
            border: none;
            border-radius: 50%;
            background: rgba(0, 0, 0, 0.7);
            color: white;
            cursor: pointer;
            z-index: 10000;
            display: none;
            font-size: 16px;
        `;
        document.body.appendChild(this.exitFullscreenBtn);
        
        this.exitFullscreenBtn.addEventListener('click', () => this.exitFullscreen());
    }

    setupEventListeners() {
        // Navigation buttons
        this.nextButton.addEventListener("click", () => this.nextStep());
        this.prevButton.addEventListener("click", () => this.prevStep());
        
        // Toolbar buttons
        this.fullscreenBtn.addEventListener('click', () => this.toggleFullscreen());
        this.laserBtn.addEventListener('click', () => this.toggleLaserMode());
        this.drawBtn.addEventListener('click', () => this.toggleDrawingMode());
        this.clearBtn.addEventListener('click', () => this.clearDrawings());
        
        // Drawing events - need to be on document for proper event handling
        document.addEventListener('mousedown', (e) => this.startDrawing(e));
        document.addEventListener('mousemove', (e) => this.draw(e));
        document.addEventListener('mouseup', () => this.stopDrawing());
        
        // Touch events for mobile drawing
        document.addEventListener('touchstart', (e) => this.startDrawing(e.touches[0]));
        document.addEventListener('touchmove', (e) => {
            e.preventDefault();
            this.draw(e.touches[0]);
        });
        document.addEventListener('touchend', () => this.stopDrawing());
        
        // Laser pointer events
        document.addEventListener('mousemove', (e) => this.updateLaserPointer(e));
        document.addEventListener('click', (e) => this.laserClick(e));
        
        // Window events
        window.addEventListener('resize', () => this.resizeCanvas());
        document.addEventListener('fullscreenchange', () => this.handleFullscreenChange());
    }

    setupKeyboardShortcuts() {
        document.addEventListener('keydown', (e) => {
            // Prevent default for our shortcuts
            switch(e.key.toLowerCase()) {
                case 'f':
                    e.preventDefault();
                    this.toggleFullscreen();
                    break;
                case 'l':
                    e.preventDefault();
                    this.toggleLaserMode();
                    break;
                case 'd':
                    e.preventDefault();
                    this.toggleDrawingMode();
                    break;
                case 'c':
                    e.preventDefault();
                    this.clearDrawings();
                    break;
                case 'g':
                    e.preventDefault();
                    this.toggleSlideSelector();
                    break;
                case 'escape':
                    e.preventDefault();
                    this.exitAllModes();
                    break;
                case 'arrowleft':
                    e.preventDefault();
                    this.prevStep();
                    break;
                case 'arrowright':
                case ' ':
                    e.preventDefault();
                    this.nextStep();
                    break;
            }
        });
    }

    setupTitleNotes() {
        document.querySelectorAll('.title-notes').forEach(noteEl => {
            const spanEl = noteEl.querySelector('span');
            noteEl.addEventListener('mouseenter', () => {
                const rect = noteEl.getBoundingClientRect();
                const viewportWidth = window.innerWidth;
                const viewportHeight = window.innerHeight;
                
                const spaceRight = viewportWidth - rect.right;
                const spaceBottom = viewportHeight - rect.bottom;
                
                if (spaceBottom < 200) {
                    spanEl.style.top = `${rect.top - 10}px`;
                    spanEl.style.transform = 'translateY(-100%)';
                } else {
                    spanEl.style.top = `${rect.bottom + 10}px`;
                    spanEl.style.transform = 'none';
                }
                
                if (spaceRight < 420) {
                    spanEl.style.left = 'auto';
                    spanEl.style.right = '20px';
                } else {
                    spanEl.style.left = `${rect.left}px`;
                    spanEl.style.right = 'auto';
                }
            });
        });
    }

    // Fullscreen functionality
    toggleFullscreen() {
        if (!this.isFullscreen) {
            this.enterFullscreen();
        } else {
            this.exitFullscreen();
        }
    }

    enterFullscreen() {
        const elem = document.documentElement;
        if (elem.requestFullscreen) {
            elem.requestFullscreen();
        } else if (elem.webkitRequestFullscreen) {
            elem.webkitRequestFullscreen();
        } else if (elem.msRequestFullscreen) {
            elem.msRequestFullscreen();
        }
    }

    exitFullscreen() {
        if (document.exitFullscreen) {
            document.exitFullscreen();
        } else if (document.webkitExitFullscreen) {
            document.webkitExitFullscreen();
        } else if (document.msExitFullscreen) {
            document.msExitFullscreen();
        }
    }

    handleFullscreenChange() {
        this.isFullscreen = !!(document.fullscreenElement || document.webkitFullscreenElement || document.msFullscreenElement);
        
        if (this.isFullscreen) {
            this.fullscreenBtn.innerHTML = '<i class="fas fa-compress"></i>';
            this.fullscreenBtn.title = 'Exit Fullscreen (F)';
            this.exitFullscreenBtn.style.display = 'block';
            document.body.classList.add('fullscreen-mode');
        } else {
            this.fullscreenBtn.innerHTML = '<i class="fas fa-expand"></i>';
            this.fullscreenBtn.title = 'Enter Fullscreen (F)';
            this.exitFullscreenBtn.style.display = 'none';
            document.body.classList.remove('fullscreen-mode');
        }
    }

    // Laser pointer functionality
    toggleLaserMode() {
        this.isLaserMode = !this.isLaserMode;
        
        if (this.isLaserMode) {
            this.laserBtn.classList.add('active');
            this.laserPointer.style.display = 'block';
            document.body.style.cursor = 'none';
            // Disable drawing mode if active
            if (this.isDrawingMode) {
                this.toggleDrawingMode();
            }
        } else {
            this.laserBtn.classList.remove('active');
            this.laserPointer.style.display = 'none';
            document.body.style.cursor = 'default';
        }
    }

    updateLaserPointer(e) {
        if (this.isLaserMode) {
            this.laserPointer.style.left = e.clientX + 'px';
            this.laserPointer.style.top = e.clientY + 'px';
        }
    }

    laserClick(e) {
        if (this.isLaserMode && !this.isDrawingMode) {
            // Prevent laser clicks on toolbar buttons
            if (e.target.closest('.presentation-toolbar') || 
                e.target.closest('.controls') || 
                e.target.closest('.exit-fullscreen-btn')) {
                return;
            }
            
            // Create click effect
            const clickEffect = document.createElement('div');
            clickEffect.style.cssText = `
                position: fixed;
                left: ${e.clientX - 15}px;
                top: ${e.clientY - 15}px;
                width: 30px;
                height: 30px;
                border: 3px solid #ff0000;
                border-radius: 50%;
                pointer-events: none;
                z-index: 9999;
                animation: laserClick 0.6s ease-out forwards;
            `;
            document.body.appendChild(clickEffect);
            
            setTimeout(() => clickEffect.remove(), 600);
        }
    }

    // Drawing functionality
    toggleDrawingMode() {
        this.isDrawingMode = !this.isDrawingMode;
        
        if (this.isDrawingMode) {
            this.drawBtn.classList.add('active');
            document.body.classList.add('drawing-mode');
            this.canvas.style.display = 'block';
            this.canvas.style.pointerEvents = 'auto';
            document.body.style.cursor = 'crosshair';
            
            // Ensure canvas is properly sized
            this.resizeCanvas();
            
            // Disable laser mode if active
            if (this.isLaserMode) {
                this.toggleLaserMode();
            }
            
            console.log('Drawing mode enabled. Canvas size:', this.canvas.width, 'x', this.canvas.height);
            console.log('Canvas element:', this.canvas);
            console.log('Canvas context:', this.ctx);
        } else {
            this.drawBtn.classList.remove('active');
            document.body.classList.remove('drawing-mode');
            this.canvas.style.display = 'none';
            this.canvas.style.pointerEvents = 'none';
            document.body.style.cursor = 'default';
            console.log('Drawing mode disabled');
        }
    }

    startDrawing(e) {
        if (!this.isDrawingMode) return;
        
        // Prevent drawing on toolbar buttons
        if (e.target.closest('.presentation-toolbar') || 
            e.target.closest('.controls') || 
            e.target.closest('.exit-fullscreen-btn')) {
            return;
        }
        
        e.preventDefault();
        this.isDrawing = true;
        
        const x = e.clientX || e.pageX;
        const y = e.clientY || e.pageY;
        
        this.currentPath = {
            points: [{ x, y }],
            color: this.ctx.strokeStyle,
            width: this.ctx.lineWidth
        };
        
        this.ctx.beginPath();
        this.ctx.moveTo(x, y);
        
        console.log('Started drawing at:', x, y);
    }

    draw(e) {
        if (!this.isDrawing || !this.isDrawingMode) return;
        
        e.preventDefault();
        
        const x = e.clientX || e.pageX;
        const y = e.clientY || e.pageY;
        
        const point = { x, y };
        this.currentPath.points.push(point);
        
        this.ctx.lineTo(x, y);
        this.ctx.stroke();
    }

    stopDrawing() {
        if (!this.isDrawing) return;
        
        this.isDrawing = false;
        if (this.currentPath && this.currentPath.points.length > 1) {
            this.drawings.push(this.currentPath);
            console.log('Saved drawing with', this.currentPath.points.length, 'points');
        }
        this.currentPath = null;
    }

    clearDrawings() {
        this.drawings = [];
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    }

    redrawCanvas() {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        
        this.drawings.forEach(path => {
            if (path.points.length < 2) return;
            
            this.ctx.strokeStyle = path.color;
            this.ctx.lineWidth = path.width;
            this.ctx.beginPath();
            this.ctx.moveTo(path.points[0].x, path.points[0].y);
            
            for (let i = 1; i < path.points.length; i++) {
                this.ctx.lineTo(path.points[i].x, path.points[i].y);
            }
            this.ctx.stroke();
        });
    }

    resizeCanvas() {
        const oldDrawings = [...this.drawings];
        this.canvas.width = window.innerWidth;
        this.canvas.height = window.innerHeight;
        
        // Restore drawing properties
        this.ctx.strokeStyle = '#ff4444';
        this.ctx.lineWidth = 3;
        this.ctx.lineCap = 'round';
        this.ctx.lineJoin = 'round';
        
        // Redraw existing drawings
        this.drawings = oldDrawings;
        this.redrawCanvas();
    }

    exitAllModes() {
        if (this.isLaserMode) this.toggleLaserMode();
        if (this.isDrawingMode) this.toggleDrawingMode();
        if (this.isFullscreen) this.exitFullscreen();
    }

    // Navigation functionality (enhanced from original)
    updateProgress() {
        const totalSlides = this.slides.length;
        const progressPercent = ((this.currentSlideIndex + 1) / totalSlides) * 100;
        this.progressIndicator.style.width = `${progressPercent}%`;

        // Update section info
        const currentSlide = this.slides[this.currentSlideIndex];
        const sectionName = currentSlide.getAttribute('data-section');
        if (sectionName) {
            this.sectionInfo.textContent = sectionName;
        } else {
            this.sectionInfo.textContent = '';
        }
        
        // Update slide counter
        this.currentSlideNum.textContent = this.currentSlideIndex + 1;
        
        // Update slide selector current slide if modal is open
        if (this.slideSelectorModal && this.slideSelectorModal.style.display !== 'none') {
            const slideItems = this.slideList.querySelectorAll('.slide-item');
            slideItems.forEach((item, index) => {
                if (index === this.currentSlideIndex) {
                    item.classList.add('current');
                } else {
                    item.classList.remove('current');
                }
            });
        }
    }

    updateButtonStates() {
        this.prevButton.disabled = this.currentSlideIndex === 0 && this.currentStepIndex === 0;

        const currentSlide = this.slides[this.currentSlideIndex];
        const steps = currentSlide.querySelectorAll(".step");

        const isLastSlide = this.currentSlideIndex === this.slides.length - 1;
        const isLastStep = steps.length > 0 ? this.currentStepIndex === steps.length - 1 : true;
        this.nextButton.disabled = isLastSlide && isLastStep;

        this.updateProgress();
    }

    showSlide(index) {
        this.slides.forEach((slide, i) => {
            slide.style.display = i === index ? "block" : "none";
        });
        this.currentStepIndex = 0;
        this.showStep(this.slides[index]);
        this.updateButtonStates();
        
        // Clear drawings when changing slides
        this.clearDrawings();
        
        // Enable scrolling for all diagrams in this slide
        this.enableScrollingForSlide(this.slides[index]);
    }

    enableScrollingForSlide(slideElement) {
        const diagramContainers = slideElement.querySelectorAll('.diagram-container');
        diagramContainers.forEach(container => {
            // Enable simple scrolling
            container.style.overflow = 'auto';
            container.style.cursor = 'default';
        });
    }

    showStep(slide) {
        const steps = Array.from(slide.querySelectorAll(".step"))
            .sort((a, b) => parseInt(a.getAttribute("step_index")) - parseInt(b.getAttribute("step_index")));

        steps.forEach((step, i) => {
            step.style.display = i === this.currentStepIndex ? "block" : "none";
        });
        this.updateButtonStates();
        
        // Enable scrolling for diagrams in the current step
        const currentStep = steps[this.currentStepIndex];
        if (currentStep) {
            this.enableScrollingForSlide(currentStep);
        }
    }

    nextStep() {
        const slide = this.slides[this.currentSlideIndex];
        const steps = slide.querySelectorAll(".step");

        if (this.currentStepIndex < steps.length - 1) {
            this.currentStepIndex++;
            this.showStep(slide);
        } else {
            if (this.currentSlideIndex < this.slides.length - 1) {
                this.currentSlideIndex++;
                this.showSlide(this.currentSlideIndex);
            }
        }
    }

    prevStep() {
        const slide = this.slides[this.currentSlideIndex];
        const steps = slide.querySelectorAll(".step");

        if (this.currentStepIndex > 0) {
            this.currentStepIndex--;
            this.showStep(slide);
        } else {
            if (this.currentSlideIndex > 0) {
                this.currentSlideIndex--;
                this.showSlide(this.currentSlideIndex);

                const newSlide = this.slides[this.currentSlideIndex];
                const newSteps = newSlide.querySelectorAll(".step");
                if (newSteps.length > 0) {
                    this.currentStepIndex = newSteps.length - 1;
                    this.showStep(newSlide);
                }
            }
        }
    }

    initializePresentation() {
        this.showSlide(this.currentSlideIndex);
        // Initialize scrolling for all visible diagrams
        this.enableScrollingForSlide(this.slides[this.currentSlideIndex]);
    }

    // Slide selector functionality
    initializeSlideSelector() {
        this.populateSlideList();
        this.setupSlideSelector();
    }

    populateSlideList() {
        this.slideList.innerHTML = '';
        
        this.slides.forEach((slide, index) => {
            const titleElement = slide.querySelector('h2');
            const title = titleElement ? titleElement.textContent : `Slide ${index + 1}`;
            
            const slideItem = document.createElement('div');
            slideItem.className = 'slide-item';
            slideItem.dataset.slideIndex = index;
            
            if (index === this.currentSlideIndex) {
                slideItem.classList.add('current');
            }
            
            slideItem.innerHTML = `
                <div class="slide-number">${index + 1}</div>
                <div class="slide-title">${title}</div>
            `;
            
            slideItem.addEventListener('click', () => {
                this.goToSlide(index);
                this.closeSlideSelector();
            });
            
            this.slideList.appendChild(slideItem);
        });
    }

    setupSlideSelector() {
        // Search functionality
        this.slideSearchInput.addEventListener('input', (e) => {
            this.filterSlides(e.target.value);
        });
        
        // Close modal when clicking outside
        this.slideSelectorModal.addEventListener('click', (e) => {
            if (e.target === this.slideSelectorModal) {
                this.closeSlideSelector();
            }
        });
        
        // Keyboard shortcuts for slide selector
        document.addEventListener('keydown', (e) => {
            if (this.slideSelectorModal.style.display !== 'none') {
                if (e.key === 'Escape') {
                    this.closeSlideSelector();
                } else if (e.key === 'Enter') {
                    const firstVisibleSlide = this.slideList.querySelector('.slide-item:not([style*="display: none"])');
                    if (firstVisibleSlide) {
                        const slideIndex = parseInt(firstVisibleSlide.dataset.slideIndex);
                        this.goToSlide(slideIndex);
                        this.closeSlideSelector();
                    }
                }
            }
        });
    }

    toggleSlideSelector() {
        if (this.slideSelectorModal.style.display === 'none' || !this.slideSelectorModal.style.display) {
            this.showSlideSelector();
        } else {
            this.closeSlideSelector();
        }
    }

    showSlideSelector() {
        this.slideSelectorModal.style.display = 'flex';
        this.populateSlideList(); // Refresh to show current slide
        this.slideSearchInput.value = '';
        this.slideSearchInput.focus();
        
        // Scroll current slide into view
        const currentSlideItem = this.slideList.querySelector('.slide-item.current');
        if (currentSlideItem) {
            currentSlideItem.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    }

    closeSlideSelector() {
        this.slideSelectorModal.style.display = 'none';
        this.slideSearchInput.value = '';
        this.filterSlides(''); // Reset filter
    }

    filterSlides(searchTerm) {
        const slideItems = this.slideList.querySelectorAll('.slide-item');
        const term = searchTerm.toLowerCase();
        
        slideItems.forEach(item => {
            const title = item.querySelector('.slide-title').textContent.toLowerCase();
            const slideNumber = item.querySelector('.slide-number').textContent;
            
            if (title.includes(term) || slideNumber.includes(term)) {
                item.style.display = 'flex';
            } else {
                item.style.display = 'none';
            }
        });
    }

    goToSlide(slideIndex) {
        if (slideIndex >= 0 && slideIndex < this.slides.length) {
            this.currentSlideIndex = slideIndex;
            this.currentStepIndex = 0;
            this.showSlide(this.currentSlideIndex);
            
            // Clear drawings when jumping to a different slide
            this.clearDrawings();
        }
    }
}

// Global functions for onclick handlers
function toggleSlideSelector() {
    if (window.presentationViewer) {
        window.presentationViewer.toggleSlideSelector();
    }
}

function closeSlideSelector() {
    if (window.presentationViewer) {
        window.presentationViewer.closeSlideSelector();
    }
}

function toggleDiagramBullets(element) {
    element.classList.toggle('active');
}

// Enhanced diagram zoom and pan functionality - GraphvizOnline style
function toggleDiagramZoom(container) {
    // Prevent zoom when in drawing or laser mode
    if (document.body.classList.contains('drawing-mode') || 
        document.body.classList.contains('laser-mode')) {
        return;
    }
    
    const isZoomed = container.classList.contains('zoomed');
    
    if (!isZoomed) {
        // Enter zoom mode
        container.classList.add('zoomed');
        document.body.style.overflow = 'hidden';
        document.addEventListener('keydown', handleZoomEscape);
        
        // Initialize pan/zoom if not already done
        if (!container.dataset.panZoomInitialized) {
            initializeAdvancedPanZoom(container);
            container.dataset.panZoomInitialized = 'true';
        }
        
        showZoomControls(container);
        
        // Add click-to-close on background
        container._backgroundClickHandler = function(e) {
            if (e.target === container) {
                toggleDiagramZoom(container);
            }
        };
        container.addEventListener('click', container._backgroundClickHandler);
        
    } else {
        // Exit zoom mode
        container.classList.remove('zoomed');
        document.body.style.overflow = '';
        document.removeEventListener('keydown', handleZoomEscape);
        hideZoomControls(container);
        
        // Remove click-to-close
        if (container._backgroundClickHandler) {
            container.removeEventListener('click', container._backgroundClickHandler);
            delete container._backgroundClickHandler;
        }
        
        // Reset diagram position
        if (container._panZoom) {
            container._panZoom.resetView();
        }
    }
}

// Simple scrolling - no pan/zoom functionality needed

// Removed complex zoom controls - using simple pan/zoom only

function handleZoomEscape(e) {
    if (e.key === 'Escape') {
        const zoomedContainer = document.querySelector('.diagram-container.zoomed');
        if (zoomedContainer) {
            toggleDiagramZoom(zoomedContainer);
        }
    }
}

// Initialize the presentation viewer when DOM is loaded
document.addEventListener("DOMContentLoaded", function () {
    window.presentationViewer = new PresentationViewer();
    console.log('PresentationViewer initialized:', window.presentationViewer);
});