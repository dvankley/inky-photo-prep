package faces

import iterator
import org.bytedeco.opencv.global.opencv_core
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.RectVector
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier
import java.awt.Rectangle

class HaarFaceDetector : FaceDetector {
    private val classifier = CascadeClassifier("models/face-detection/haarcascade_frontalface_alt.xml");

    override fun findFaces(targetImage: Mat): List<DnnFaceDetector.Face> {
        val height = targetImage.rows()
        val width = targetImage.cols()
        val grayImage = Mat(height, width, opencv_core.CV_8UC1)

        // Let's try to detect some faces! but we need a grayscale image...
        opencv_imgproc.cvtColor(targetImage, grayImage, opencv_imgproc.CV_BGR2GRAY)
        val faces = RectVector()
        classifier.detectMultiScale(grayImage, faces)
        val total = faces.size()
        return faces.iterator().map { DnnFaceDetector.Face(
            1.0F,
            Rectangle(it.x(), it.y(), it.width(), it.height())
        ) }.toList()
    }
}