package net.djvk.inkyPhotoPrep.faces

import org.bytedeco.javacpp.indexer.FloatIndexer
import org.bytedeco.opencv.global.opencv_core.CV_32F
import org.bytedeco.opencv.global.opencv_core.minMaxLoc
import org.bytedeco.opencv.global.opencv_dnn.blobFromImage
import org.bytedeco.opencv.global.opencv_dnn.readNetFromCaffe
import org.bytedeco.opencv.global.opencv_imgproc.resize
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Point
import org.bytedeco.opencv.opencv_core.Scalar
import org.bytedeco.opencv.opencv_core.Size
import java.awt.Rectangle
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt


class DnnFaceDetector : FaceDetector {
    data class Face(
        val confidence: Float,
        val box: Rectangle,
    )

    private val PROTO_FILE = "models/face-detection/deploy.prototxt"
    private val CAFFE_MODEL_FILE = "models/face-detection/res10_300x300_ssd_iter_140000.caffemodel"

    private val net = readNetFromCaffe(PROTO_FILE, CAFFE_MODEL_FILE)
//        "models/face-detection/bvlc_googlenet.prototxt",
//        "models/face-detection/bvlc_googlenet.caffemodel"
//    )

    override fun findFaces(targetImage: Mat): List<Face> {
        val height = targetImage.rows()
        val width = targetImage.cols()
        //! [Initialize network]

        //! [Prepare blob]
//        resize(targetImage, targetImage, Size(224, 224)) //GoogLeNet accepts only 224x224 RGB-images
//        val inputBlob = blobFromImage(targetImage) //Convert Mat to 4-dimensional dnn::Blob from image
        //! [Prepare blob]


        //! [Set input blob]
//        net.setInput(inputBlob, "data", 1.0, null) //set the network input
        //! [Set input blob]


        resize(targetImage, targetImage, Size(300, 300)) //resize the image to match the input size of the model

        //create a 4-dimensional blob from image with NCHW (Number of images in the batch -for training only-, Channel, Height, Width) dimensions order,
        //for more detailes read the official docs at https://docs.opencv.org/trunk/d6/d0f/group__dnn.html#gabd0e76da3c6ad15c08b01ef21ad55dd8
        val blob: Mat = blobFromImage(targetImage, 1.0, Size(300, 300), Scalar(104.0, 177.0, 123.0, 0.0), false, false, CV_32F)

        net.setInput(blob) //set the input to network model

//        val output = net.forward() //feed forward the input to the netwrok to get the output matrix
        val prob = net.forward() //feed forward the input to the netwrok to get the output matrix


//
//        cv::Mat detection = net.forward("detection_out");
//
//        cv::Mat detectionMat(detection.size[2], detection.size[3], CV_32F, detection.ptr<float>());
//
//        for(int i = 0; i &lt; detectionMat.rows; i++)
//        {
//            float confidence = detectionMat.at&lt;float&gt;(i, 2);
//
//            if(confidence &gt; confidenceThreshold)
//            {
//                int x1 = static_cast<int>(detectionMat.at<float>(i, 3) * frameWidth);
//                int y1 = static_cast<int>(detectionMat.at<float>(i, 4) * frameHeight);
//                int x2 = static_cast<int>(detectionMat.at<float>(i, 5) * frameWidth);
//                int y2 = static_cast<int>(detectionMat.at<float>(i, 6) * frameHeight);
//
//                cv::rectangle(frameOpenCVDNN, cv::Point(x1, y1), cv::Point(x2, y2), cv::Scalar(0, 255, 0),2, 4);
//            }
//        }


        //! [Make forward pass]
//        val prob = net.forward("prob") //compute output

        val faces = mutableListOf<Face>()

        val ne = Mat(
            Size(prob.size(3), prob.size(2)),
            CV_32F,
            prob.ptr(0, 0)
        ) //extract a 2d matrix for 4d output matrix with form of (number of detections x 7)


        val srcIndexer = ne.createIndexer<FloatIndexer>() // create indexer to access elements of the matric
        val faceCount = prob.size(3)

        for (i in 0 until faceCount) { //iterate to extract elements
            val confidence = srcIndexer[i.toLong(), 2]
            val f1 = srcIndexer[i.toLong(), 3]
            val f2 = srcIndexer[i.toLong(), 4]
            val f3 = srcIndexer[i.toLong(), 5]
            val f4 = srcIndexer[i.toLong(), 6]
//            if (confidence > .6) {
            val tx = f1 * width //top left point's x
            val ty = f2 * height //top left point's y
            val bx = f3 * width //bottom right point's x
            val by = f4 * height //bottom right point's y
            faces.add(
                Face(
                    confidence,
                    Rectangle(
                        tx.roundToInt(),
                        ty.roundToInt(),
                        (bx - tx).roundToInt(),
                        (by - ty).roundToInt()
                    ),
                )
            )
//            }
        }


//        val sizes = prob.size()
//        val dump = prob.data()
//        val pIndexer = prob.createIndexer<FloatRawIndexer>(true)
//        val thirdDimension = pIndexer.get(0, 0)
//        val meep = pIndexer.get(0, 0, 0, 2).toRawBits()
//        val beep = pIndexer.get(0, 0, 0, 3).toRawBits()
//        val feep = pIndexer.get(0, 0, 0, 4).toRawBits()
//        val leep = pIndexer.get(0, 0, 0, 6).toRawBits()
////        val thirdDimension = prob.ptr(0, 0)
//        val thing = Mat(thirdDimension)
//        val faceCount = thing.cols()
//        val fIndexer = thing.createIndexer<FloatRawIndexer>(true)
//        for (i in 0L until faceCount) {
//            val a = fIndexer.get(i, 3L)
//            val b = fIndexer.get(i, 4L)
//            val c = fIndexer.get(i, 5L)
//            val d = fIndexer.get(i, 6L)
//
//            faces.add(Face(
//                fIndexer.get(i, 2L),
//                Rectangle(1, 1, 1, 1)
//            ))
//
//        }
//        val r = thing.rows()
//        val c = thing.cols()
//        val face1 = thing.col(0)
//        val conf = face1.col(2)
//        val box =
//            face1.col(2)
//        val fIndexer = thing.createIndexer<FloatRawIndexer>()

        //! [Make forward pass]

        //! [Gather output]
//        prob.
        val classId = Point()
        val classProb = DoubleArray(1)
        getMaxClass(prob, classId, classProb) //find the best class
        //! [Gather output]

        //! [Print results]
//        val classNames: List<String> = readClassNames()

//        println("Best class: #" + classId.x() + " '" + classNames[classId.x()] + "'")
        println("Best class: #" + classId.x())
        println("Probability: " + classProb[0] * 100 + "%")
        //! [Print results]    }

        return faces
    }

    /* Find best class for the blob (i. e. class with maximal probability) */
    fun getMaxClass(probBlob: Mat, classId: Point?, classProb: DoubleArray?) {
        val probMat = probBlob.reshape(1, 1) //reshape the blob to 1x1000 matrix
        minMaxLoc(probMat, null, classProb, null, classId, null)
    }

    fun readClassNames(): List<String> {
        val filename = "synset_words.txt"
        val classNames = mutableListOf<String>()
        try {
            File(filename).useLines { lines ->
                for (line in lines) {
                    classNames.add(line.substring(line.indexOf(' ') + 1))
                }
            }
//            BufferedReader(FileReader(File(filename))).use { br ->
//                classNames = ArrayList()
//                var name: String? = null
//                while (br.readLine().also { name = it } != null) {
//                    classNames.add(name!!.substring(name!!.indexOf(' ') + 1))
//                }
//            }
        } catch (ex: IOException) {
            System.err.println("File with classes labels not found $filename")
            System.exit(-1)
        }
        return classNames
    }
}