import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.sl.usermodel.PictureData

class PPTGenerator {
    static void generatePPT(File outputDir, File pptFile) {
        def slideShow = new XMLSlideShow()

        // Get all subdirectories
        def allSubDirs = outputDir.listFiles().findAll { it.isDirectory() }
        println "Found ${allSubDirs.size()} subdirectories"

        // Process each subdirectory independently
        allSubDirs.each { subDir ->
            println "Processing directory: ${subDir.name}"
            
            // Find max step for this subdirectory
            def maxStep = subDir.listFiles().findAll { file -> 
                file.name.endsWith('.png') && file.name =~ /step\d+/
            }.collect { file ->
                def matcher = file.name =~ /step(\d+)/
                matcher.find() ? matcher[0][1].toInteger() : 0
            }.max() ?: 0

            println "  Found ${maxStep} steps in ${subDir.name}"

            // Process each step for this subdirectory
            (1..maxStep).each { stepNum ->
                println "  Processing step ${stepNum}"
                
                // Look specifically for step{N}.png
                def stepFile = new File(subDir, "step${stepNum}.png")
                if (stepFile.exists()) {
                    println "    Adding image: ${stepFile.name}"
                    def slide = slideShow.createSlide()

                    def pictureData = slideShow.addPicture(
                        stepFile.bytes, 
                        PictureData.PictureType.PNG
                    )
                    slide.createPicture(pictureData)
                } else {
                    println "    Warning: Missing step${stepNum}.png in ${subDir.name}"
                }
            }
        }

        pptFile.withOutputStream { outputStream ->
            slideShow.write(outputStream)
        }
        
        println "Generated PowerPoint presentation: ${pptFile.absolutePath}"
    }
}