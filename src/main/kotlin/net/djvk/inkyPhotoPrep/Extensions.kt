package net.djvk.inkyPhotoPrep

import org.bytedeco.opencv.opencv_core.Point2d
import org.bytedeco.opencv.opencv_core.Rect
import org.bytedeco.opencv.opencv_core.RectVector
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit


fun String.runCommand(timeoutSeconds: Long, workingDir: File = File("."), envVars: Map<String, String> = mapOf()): String? {
    return try {
        val parts = this.split("\\s".toRegex())
        val builder = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)

        val env = builder.environment()
        envVars.forEach { (name, value) -> env[name] = value }

        val proc = builder.start()
        proc.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        if (proc.exitValue() != 0) {
            throw RuntimeException("Command '$this' exited with code ${proc.exitValue()} and output: \n${proc.inputStream.bufferedReader().readText()}")
        }
        proc.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
        null
    }
}

fun RectVector.iterator(): Sequence<Rect> {
    return sequence {
        val total = this@iterator.size()
        for (i in 0 until total) {
            yield(this@iterator.get(i))
        }
//        val iterator = this@iterator.begin()
//        var el: Rect
//        do {
//            el = iterator.get()
//            yield(el)
//            iterator.increment()
//        } while (iterator.position() != iterator.limit())
    }
}

fun Rect.center(): Point2d {
    return Point2d(this.x() + (this.width() / 2.0), this.y() + (this.height() / 2.0))
}

fun Image.toBufferedImage(): BufferedImage {
    if (this is BufferedImage) {
        return this
    }
    val bufferedImage = BufferedImage(this.getWidth(null), this.getHeight(null), BufferedImage.TYPE_INT_ARGB)

    val graphics2D = bufferedImage.createGraphics()
    graphics2D.drawImage(this, 0, 0, null)
    graphics2D.dispose()

    return bufferedImage
}
