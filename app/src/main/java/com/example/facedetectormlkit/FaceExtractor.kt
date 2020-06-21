package com.example.facedetectormlkit

import android.graphics.Bitmap
import android.media.Image
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions

class FaceExtractor(){
    private val face: Bitmap?= null

    val highAccuracyFaceDetectorOpts = FirebaseVisionFaceDetectorOptions.Builder()
        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
        .build()
    private val firebaseVision:FirebaseVision = FirebaseVision.getInstance()
    private val firebaseFaceDetector = firebaseVision.getVisionFaceDetector(highAccuracyFaceDetectorOpts)


    fun getFaces(image:Image, rotation: Int): Task<List<FirebaseVisionFace>> {
        val firebaseImage = FirebaseVisionImage.fromMediaImage(image,rotation )
        return firebaseFaceDetector.detectInImage(firebaseImage)
    }




}