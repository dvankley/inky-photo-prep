package net.djvk.inkyPhotoPrep

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.djvk.inkyPhotoPrep.dithering.ErrorDiffusion
import net.djvk.inkyPhotoPrep.dithering.YliluomaKnollPattern
import net.djvk.inkyPhotoPrep.dithering.inkyPalette
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.Path

fun main(args: Array<String>) {
    println("Dithering test")

//    val ditherer = ErrorDiffusion(inkyPalette, ErrorDiffusion.DiffusionMap.Atkinson)
    val ditherer = YliluomaKnollPattern(inkyPalette)
    val inputPath = Path("input")
    val outputPath = Path("output")

    runBlocking {
        val files = inputPath.toFile().listFiles(PhotoFilenameFilter)
            ?: throw IllegalArgumentException("Input directory empty")

        files.forEachIndexed { index, file ->
            println("Sending $file to channel with index $index")

            val logPrefix = "\t${file.name}:"
            println("$logPrefix Loading input image")
            val img = readAndRotateImage2(file)

            val width = img.width
            val height = img.height
            val targetWidth = (width * .2).toInt()
            val targetHeight = (height * .2).toInt()

            val tmpScaled = img.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT)
            val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
            scaled.graphics.drawImage(tmpScaled, 0, 0, null)

            println("$logPrefix Dithering image")
            val dithered = ditherer.dither(scaled)

            println("$logPrefix Writing output file")
            withContext(Dispatchers.IO) {
                ImageIO.write(dithered, "jpg", outputPath.resolve(file.name).toFile())
            }

        }
    }
}


suspend fun readAndRotateImage2(file: File): BufferedImage {
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
