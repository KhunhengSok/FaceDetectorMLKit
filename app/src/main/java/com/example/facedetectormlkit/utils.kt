package com.example.facedetectormlkit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import android.util.SparseArray
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.facedetectormlkit.FaceDetectorImageAnalyzer.Companion.TAG
import com.google.android.gms.vision.face.Face
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import java.lang.ref.WeakReference



fun showToat(activity: WeakReference<Context>, message: String, length: Int =  Toast.LENGTH_SHORT){
    Toast.makeText(activity.get(), message, length).show()
}
fun drawRectangle(faces: List<FirebaseVisionFace>, bitmap: Bitmap,  recPaint: Paint){
    var tempCanvas: Canvas
    for ( face in faces){
//        var x1:Float = face.boundingBox.
//        var y1:Float =
//        var x2:Float =
//        var y2:Float =
//        tempCanvas.drawRoundRect(RectF(x1, y1, x2, y2), 2.0f,2.0f , recPaint)
        var box = face.boundingBox
        tempCanvas = Canvas(bitmap)
//        tempCanvas.drawRoundRect(RectF(box), 2.0f,2.0f , recPaint)
        tempCanvas.drawRoundRect(RectF(1.0f,1.0f,100.0f,100.0f), 2.0f,2.0f , recPaint)
        Log.i(TAG, "Drew")

    }
}

