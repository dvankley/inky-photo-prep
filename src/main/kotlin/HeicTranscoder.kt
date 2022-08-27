import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.io.File
import java.io.FilenameFilter
import java.nio.file.Path
import java.util.*

class HeicTranscoder(private val imageDirectory: Path) {
    private val transcodeWorkerCount = 30
    private val HeicFilenameFilter = FilenameFilter { _, name ->
        name.lowercase(Locale.getDefault()).contains(".heic")
    }

    suspend fun convertAllHeicFiles() {
        val magickCheck = "magick --version".runCommand(1)
        if (magickCheck.isNullOrEmpty() || magickCheck.contains("command not found")) {
            throw RuntimeException("ImageMagick is not installed, please install it (i.e. 'brew install imagemagick' on mac).")
        }

        val heicFileList = imageDirectory.toFile().listFiles(HeicFilenameFilter) ?: arrayOf()
        println("Found ${heicFileList.size} HEIC files to convert")
        coroutineScope {
            heicFileList
                .map {
                    launch { convertHeic(it) }
                }
                .joinAll()
        }
    }

    private suspend fun convertHeic(heicPath: File) {
        val heicFilename = heicPath.name
        val rootFilename = heicPath.nameWithoutExtension
        val jpgFilename = "$rootFilename.jpg"
        println("Converting HEIC file $heicFilename to $jpgFilename")

        "magick convert $heicFilename $jpgFilename".runCommand(30, imageDirectory.toFile())

        println("Conversion successful, deleting HEIC file $heicFilename")
        File("$imageDirectory/$heicFilename").delete()
    }
}