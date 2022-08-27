package faces

import org.bytedeco.opencv.opencv_core.Mat

interface FaceDetector {
    fun findFaces(targetImage: Mat): List<DnnFaceDetector.Face>
}