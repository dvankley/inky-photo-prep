package net.djvk.inkyPhotoPrep

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import net.djvk.inkyPhotoPrep.encoding.InkyFramebufferEncoder
import net.djvk.inkyPhotoPrep.faces.DnnFaceDetector
import net.djvk.inkyPhotoPrep.faces.FaceDetector
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import net.djvk.inkyPhotoPrep.dithering.ErrorDiffusion
import net.djvk.inkyPhotoPrep.dithering.YliluomaKnollPattern
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.opencv.global.opencv_imgcodecs.imread
import java.awt.*
import java.awt.image.BufferedImage
import java.awt.image.RasterFormatException
import java.io.File
import java.io.FilenameFilter
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.writeBytes
import kotlin.math.roundToInt


val PhotoFilenameFilter = FilenameFilter { _, name ->
    val lower = name.lowercase()
    name != ".DS_Store" &&
            !lower.endsWith("mov") &&
            !lower.endsWith("mp4")
}

enum class OutputType {
    RpiJpeg,
    InkyPicoBinary,
}

enum class DisplayModel {
    Inky57,
    Inky73,
}

val outputType = OutputType.RpiJpeg
val model = DisplayModel.Inky73


/**
 * Set to true to write to output a copy of each input image with boxes drawn around each recognized face,
 *  with brightness proportional to confidence.
 */
const val FACE_DETECTION_DEBUG = false

/**
 * Set to desired dithering class
 */
val ditherer = ErrorDiffusion(getPalette(model), ErrorDiffusion.DiffusionMap.Atkinson)

/**
 * Choose your preferred face net.djvk.inkyPhotoPrep.getDetector here.
 * There honestly isn't much point in using the haar net.djvk.inkyPhotoPrep.getDetector.
 */
val detector = DnnFaceDetector()

/**
 * Width dimension of target display
 * Inky 5.7 is 600
 * Inky 7.3 is 800
 */
val targetWidth = when (model) {
    DisplayModel.Inky57 -> 600
    DisplayModel.Inky73 -> 800
}

/**
 * Height dimension of target display
 * Inky 5.7 is 448
 * Inky 7.3 is 480
 */
val targetHeight = when (model) {
    DisplayModel.Inky57 -> 448
    DisplayModel.Inky73 -> 480
}
val targetAspectRatio = targetWidth / targetHeight.toDouble()

/**
 * How many processing workers to use. Recommended setting is the number of CPU cores you have.
 */
const val workerCount = 10

/**
 * For the DNN face matcher, the level of confidence to consider a face we should crop to.
 */
const val faceConfidenceThreshold = .6

/**
 * For the DNN face matcher, if no faces were found at [faceConfidenceThreshold], we lower our standards
 * to this threshold and try again.
 */
const val lowerFaceConfidenceThreshold = .3

fun main(args: Array<String>) {
    println("Photo prep started")

    val inputPath = Path(
        args.firstOrNull()
            ?: throw IllegalArgumentException("First argument should be the input path")
    )
    val outputPath = Path(args[1])

    runBlocking {
        val heic = HeicTranscoder(inputPath)
        heic.convertAllHeicFiles()
    }

    runBlocking {
        val inputFileChannel = Channel<Pair<Int, File>>(Channel.Factory.UNLIMITED)
        val files = inputPath.toFile().listFiles(PhotoFilenameFilter)
            ?: throw IllegalArgumentException("Input directory empty")

        files.forEachIndexed { index, file ->
            println("Sending $file to channel with index $index")
            inputFileChannel.send(Pair(index, file))
        }
        inputFileChannel.close()

        // Process in parallel
        val processingWorkers = (1..workerCount).map {
            imageProcessingWorker(inputFileChannel, detector, outputPath)
        }
        processingWorkers.joinAll()
    }
}

private fun CoroutineScope.imageProcessingWorker(
    fileChannel: ReceiveChannel<Pair<Int, File>>,
    detector: FaceDetector,
    outputPath: Path,
) = launch {
    for ((index, file) in fileChannel) {
        val logPrefix = "\t${file.name}:"
        println("$logPrefix Loading input image")
        val img = readAndRotateImage(file)

        val width = img.width
        val height = img.height

        // Detect face(s)
        println("$logPrefix Detecting faces")
        val center = getCenterOfFaces(file, detector, img)

        println("$logPrefix Cropping image")
        val croptangle = getCropParametersForCenter(center, width, height, targetAspectRatio)
        val croppedImg: BufferedImage
        try {
            croppedImg = img.getSubimage(
                croptangle.x,
                croptangle.y,
                croptangle.width,
                croptangle.height,
            )
        } catch (rfe: RasterFormatException) {
            println("$rfe\nFailed to crop $file with $width,$height ; cropping to $croptangle")
            continue
        }

        val tmpScaled = croppedImg.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT)
        val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        scaled.graphics.drawImage(tmpScaled, 0, 0, null)

        println("$logPrefix Dithering image")
        val dithered = ditherer.dither(scaled)

        println("$logPrefix Writing output file")

        when (outputType) {
            OutputType.RpiJpeg -> {
                // Write to output directory
                withContext(Dispatchers.IO) {
                    ImageIO.write(dithered, "jpg", outputPath.resolve(file.name).toFile())
                }
            }
            OutputType.InkyPicoBinary -> {
                val encoder = InkyFramebufferEncoder(targetWidth, targetHeight, getPalette(model))
                // Write to output directory
                withContext(Dispatchers.IO) {
                    outputPath.resolve("${index}.bin").writeBytes(encoder.encode(dithered))
                }
            }
        }
    }
}

suspend fun getCenterOfFaces(file: File, detector: FaceDetector, img: BufferedImage): Point {
    val mat = imread(file.toString())
    val rects = detector.findFaces(mat)
    return getCropCenterTarget(rects, file, img)
}

suspend fun getCropParametersForCenter(
    center: Point,
    width: Int,
    height: Int,
    targetAspectRatio: Double
): Rectangle {
    // Crop to desired aspect ratio around the target
    val targetWidth: Double
    val targetHeight: Double
    if (width > height) {
        // Height is smaller, so crop to fit the height
        targetHeight = height.toDouble()
        targetWidth = (targetAspectRatio * targetHeight).coerceAtMost(width.toDouble())
    } else {
        // Width is smaller, so crop to fit the width
        targetWidth = width.toDouble()
        targetHeight = (targetWidth / targetAspectRatio).coerceAtMost(height.toDouble())
    }

    val targetX = (center.x - (targetWidth / 2.0))
        .coerceAtMost(width - targetWidth)
        .coerceAtLeast(0.0)
    val targetY = (center.y - (targetHeight / 2.0))
        .coerceAtMost(height - targetHeight)
        .coerceAtLeast(0.0)

    return Rectangle(targetX.roundToInt(), targetY.roundToInt(), targetWidth.roundToInt(), targetHeight.roundToInt())
}

suspend fun readAndRotateImage(file: File): BufferedImage {
    val img = withContext(Dispatchers.IO) {
        ImageIO.read(file)
    } ?: throw RuntimeException("Failed to read image $file")
    val metadata = withContext(Dispatchers.IO) {
        ImageMetadataReader.readMetadata(file)
    }

    val exifIFD0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
        ?: return img

    if (!exifIFD0.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
        return img
    }

    return when (val orientation = exifIFD0.getInt(ExifIFD0Directory.TAG_ORIENTATION)) {
        // From com.drew.metadata.TagDescriptor.getOrientationDescription
//        "Top, left side (Horizontal / normal)",
        1 -> img
//        "Top, right side (Mirror horizontal)",
//        "Bottom, right side (Rotate 180)",
        3 -> return Rotation.CLOCKWISE_180.rotate(img)
//        "Bottom, left side (Mirror vertical)",
//        "Left side, top (Mirror horizontal and rotate 270 CW)",
//        "Right side, top (Rotate 90 CW)",
        6 -> return Rotation.CLOCKWISE_90.rotate(img)
//        "Right side, bottom (Mirror horizontal and rotate 90 CW)",
//        "Left side, bottom (Rotate 270 CW)");
        8 -> return Rotation.CLOCKWISE_270.rotate(img)
        else -> throw IllegalArgumentException("Invalid EXIF orientation index $orientation")
    }
}

suspend fun getCropCenterTarget(faces: List<DnnFaceDetector.Face>, file: File, img: BufferedImage): Point {
    if (FACE_DETECTION_DEBUG) {
        val debugImg = Java2DFrameConverter.cloneBufferedImage(img)
        val graph: Graphics2D = debugImg.createGraphics()
        graph.stroke = BasicStroke(20.0f)
        val greenHsbs = FloatArray(3)
        Color.RGBtoHSB(Color.GREEN.red, Color.GREEN.green, Color.GREEN.blue, greenHsbs)
        faces.forEach { face ->
            graph.paint =
                Color.getHSBColor(greenHsbs[0], greenHsbs[1], ((greenHsbs[2] + 1.0) * face.confidence).toFloat())
            graph.drawRect(face.box.x, face.box.y, face.box.width, face.box.height)
        }
        graph.dispose()
        withContext(Dispatchers.IO) {
            ImageIO.write(debugImg, "jpg", File("output/${file.name}_debug.jpg"))
        }
    }

    var filteredFaces = faces.filter { it.confidence > faceConfidenceThreshold }

    // Ok, then let's lower our standards
    if (filteredFaces.isEmpty()) {
        filteredFaces = faces.filter { it.confidence > lowerFaceConfidenceThreshold }
    }
    return if (filteredFaces.isEmpty()) {
        // No faces detected, just use the net.djvk.inkyPhotoPrep.center of the photo
        Point((img.width / 2.0).roundToInt(), (img.height / 2.0).roundToInt())
    } else {
        // Average out the net.djvk.inkyPhotoPrep.center of all the faces to get our target
        var faceCount = 0
        var totalCenterX = 0.0
        var totalCenterY = 0.0
        filteredFaces.iterator().forEach { face ->
            faceCount++
            totalCenterX += face.box.centerX
            totalCenterY += face.box.centerY
        }
        Point((totalCenterX / faceCount).roundToInt(), (totalCenterY / faceCount).roundToInt())
    }
}

fun getPalette(model: DisplayModel): Array<Color> {
    val basePalette = arrayOf(
            Color(0, 0, 0),
            Color(255, 255, 255),
            Color(0, 255, 0),
            Color(0, 0, 255),
            Color(255, 0, 0),
            Color(255, 255, 0),
            Color(255, 128, 0),
        )
    // From https://github.com/pimoroni/pimoroni-pico/issues/681#issuecomment-1440469730
    return when (model) {
        DisplayModel.Inky57 -> basePalette + Color(220, 180, 200)
        // The 7.3 doesn't have the "taupe" clear color
        // See https://github.com/pimoroni/pimoroni-pico/blob/main/micropython/modules_py/inky_frame.md#colour--dithering
        DisplayModel.Inky73 -> basePalette
    }
}
