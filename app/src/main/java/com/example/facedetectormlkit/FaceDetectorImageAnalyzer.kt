package com.example.facedetectormlkit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.Image
import android.util.Log
import android.util.SparseIntArray
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.drawToBitmap
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference
import kotlin.math.log

interface FaceDetectorListener{
    fun onDetect(result: Task<List <FirebaseVisionFace>>, image: Image )
}


class FaceDetectorImageAnalyzer(val activityContext: WeakReference<AppCompatActivity>): ImageAnalysis.Analyzer {
    private val ORIENTATIONS = SparseIntArray()
    private  val firebaseVision: FirebaseVision = FirebaseVision.getInstance()
    val options = FirebaseVisionFaceDetectorOptions.Builder().enableTracking().build()
    private var  image: Image?= null

    private var canvas: Canvas = Canvas()
    private var paint:Paint = Paint()
    private lateinit var myBitmap:Bitmap

    init{
        paint.strokeWidth = 5.0f
        paint.color = Color.GREEN
        paint.style = Paint.Style.STROKE

    }
    companion object {
        const  val TAG = "FACE_DETECTOR"
    }

    //High accuracy, all landmarks, and both smile and eye-open classification
    val highAccuracyFaceDetectorOpts = FirebaseVisionFaceDetectorOptions.Builder()
        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
        .build()

    private val realtimeOpts = FirebaseVisionFaceDetectorOptions.Builder()
//        .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
        .build()
    private val detector =  firebaseVision.getVisionFaceDetector(realtimeOpts)


    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        image  = imageProxy.image

        if ( image != null) {
            val visionImage = FirebaseVisionImage.fromMediaImage(image!!, activityContext.get()?.windowManager?.defaultDisplay?.rotation!!);

            detector.detectInImage(visionImage).addOnSuccessListener {
                Log.i(FaceDetectorImageAnalyzer.TAG, "Face found: " + it.size.toString())

               if(it.size >  0){

                   var viewFinder =( activityContext.get() as MainActivity).viewFinder
//                   myBitmap = Bitmap.createBitmap(image!!.width, image!!.height, Bitmap.Config.RGB_565)
                   myBitmap = viewFinder.drawToBitmap( Bitmap.Config.ARGB_8888)

//                   myBitmap = Bitmap.createBitmap(image!!.width, image!!.height, Bitmap.Config.RGB_565)
                   var overlay = Bitmap.createBitmap(myBitmap.width, myBitmap.height, Bitmap.Config.ARGB_8888)
                   canvas = Canvas( overlay)
                   drawRectangle(it, myBitmap, paint)
               }
                    /*for(face in it){
                        var box = face.boundingBox
                        imageProxy.
                    }*/
//                            for (face in it){
//                                val bounds = face.boundingBox
//                                val rotY = face.headEulerAngleY
//                                val rotZ = face.headEulerAngleZ
//
//                                //landmarks
//                                var leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR)
//                                //                    var rightEar = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR)
//                                leftEar.apply {
//                                    val leftEarPost = (leftEar)
//                                }
//
//                                //contour
//                                var leftEyeContour = face.getContour(FirebaseVisionFaceContour.LEFT_EYE)
//                                val upperLipbottomContour = face.getContour(
//                                    FirebaseVisionFaceContour.UPPER_LIP_BOTTOM)
//
//                                //classification
//                                if( face.smilingProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY){
//                                    val smileProb = face.smilingProbability
//                                }
//                                if (face.rightEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY){
//                                    val rightEyeProb = face.rightEyeOpenProbability
//                                }
//
//
//                            }
                imageProxy.close()

            }.addOnFailureListener{
                Toast.makeText(activityContext.get(), "No face detected.", Toast.LENGTH_SHORT).show()
                Log.e(FaceDetectorImageAnalyzer.TAG, "No face found.")
                imageProxy.close()

            }

        }
    }
}









/*
.addOnSuccessListener {
                listener.onDetect(it)
                Log.i(TAG, "Face found: " + it.size.toString())

                drawRectangle(it, canvas, paint)

                for (face in it){
                    val bounds = face.boundingBox
                    val rotY = face.headEulerAngleY
                    val rotZ = face.headEulerAngleZ

                    //landmarks
                    var leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR)
                    //                    var rightEar = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR)
                    leftEar.apply {
                        val leftEarPost = (leftEar)
                    }

                    //contour
                    var leftEyeContour = face.getContour(FirebaseVisionFaceContour.LEFT_EYE)
                    val upperLipbottomContour = face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM)

                    //classification
                    if( face.smilingProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY){
                        val smileProb = face.smilingProbability
                    }
                    if (face.rightEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY){
                        val rightEyeProb = face.rightEyeOpenProbability
                    }

                }
            }.addOnFailureListener{
                Toast.makeText(activityContext.get(), "No face detected.", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "No face found.")
            }
 */