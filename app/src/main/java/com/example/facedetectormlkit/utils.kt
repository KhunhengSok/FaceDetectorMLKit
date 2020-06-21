package com.example.facedetectormlkit

import android.app.Activity
import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.graphics.*
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image
import android.os.Build
import android.util.Log
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.Surface
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.facedetectormlkit.FaceDetectorImageAnalyzer.Companion.TAG
import com.example.facedetectormlkit.utils.cosineSimilarity
import com.google.android.gms.vision.face.Face
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import org.tensorflow.lite.support.image.TensorImage
import java.lang.ref.WeakReference



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

    }
}

fun rotationDegreesToFirebaseRotation(rotationDegrees: Int) = when (rotationDegrees) {
    0 -> FirebaseVisionImageMetadata.ROTATION_0
    90 -> FirebaseVisionImageMetadata.ROTATION_90
    180 -> FirebaseVisionImageMetadata.ROTATION_180
    270 -> FirebaseVisionImageMetadata.ROTATION_270
    else -> throw IllegalArgumentException("Rotation $rotationDegrees not supported")
}





/**
 * Get the angle by which an image must be rotated given the device's current
 * orientation.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
@Throws(CameraAccessException::class)
fun getRotationCompensation(cameraId: String, activity: Activity, context: Context): Int {
    val ORIENTATIONS = SparseIntArray()
    ORIENTATIONS.append(Surface.ROTATION_0, 90)
    ORIENTATIONS.append(Surface.ROTATION_90, 0)
    ORIENTATIONS.append(Surface.ROTATION_180, 270)
    ORIENTATIONS.append(Surface.ROTATION_270, 180)


    // Get the device's current rotation relative to its "native" orientation.
    // Then, from the ORIENTATIONS table, look up the angle the image must be
    // rotated to compensate for the device's rotation.
    val deviceRotation = activity.windowManager.defaultDisplay.rotation
    var rotationCompensation = ORIENTATIONS.get(deviceRotation)

    // On most devices, the sensor orientation is 90 degrees, but for some
    // devices it is 270 degrees. For devices with a sensor orientation of
    // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
    val cameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager
    val sensorOrientation = cameraManager
        .getCameraCharacteristics(cameraId)
        .get(CameraCharacteristics.SENSOR_ORIENTATION)!!
    rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360

    // Return the corresponding FirebaseVisionImageMetadata rotation value.
    val result: Int
    when (rotationCompensation) {
        0 -> result = FirebaseVisionImageMetadata.ROTATION_0
        90 -> result = FirebaseVisionImageMetadata.ROTATION_90
        180 -> result = FirebaseVisionImageMetadata.ROTATION_180
        270 -> result = FirebaseVisionImageMetadata.ROTATION_270
        else -> {
            result = FirebaseVisionImageMetadata.ROTATION_0
            Log.e(TAG, "Bad rotation value: $rotationCompensation")
        }
    }
    return result
}

//fun convertImageToBitmap(image: Image, output:Array<Int>, cachedYuvImage: Array<Array<Byte>>){
//    if(cachedYuvImage == null || cachedYuvImage.size != 3){
//        var a = arrayOf(Byte)
//        var b = arrayOf(Byte)
//        var c = arrayOf(Byte)
//        cachedYuvImage = arrayOf(a, b, c)
//    }
//}


//int[] convertImageToBitmap(Image image, int[] output, byte[][] cachedYuvBytes) {
//    if (cachedYuvBytes == null || cachedYuvBytes.length != 3) {
//        cachedYuvBytes = new byte[3][];
//    }
//    Image.Plane[] planes = image.getPlanes();
//    fillBytes(planes, cachedYuvBytes);
//
//    final int yRowStride = planes[0].getRowStride();
//    final int uvRowStride = planes[1].getRowStride();
//    final int uvPixelStride = planes[1].getPixelStride();
//
//    convertYUV420ToARGB8888(cachedYuvBytes[0], cachedYuvBytes[1], cachedYuvBytes[2],
//        image.getWidth(), image.getHeight(), yRowStride, uvRowStride, uvPixelStride, output);
//    return output;
//}

