package com.example.facedetectormlkit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.*
import android.media.Image
import android.media.Image.Plane
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.example.facedetectormlkit.TensorflowUtils.ImageUtils
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean


class FaceDetectorImageAnalyzer(
    val activityContext: WeakReference<MainActivity>,
    val faceEmbedding: FaceEmbedding,
    val faceBoxListener: FaceBoxListener
) : ImageAnalysis.Analyzer {
    private val firebaseVision: FirebaseVision = FirebaseVision.getInstance()
    val options = FirebaseVisionFaceDetectorOptions.Builder().enableTracking().build()
    private var image: Image? = null

    private var canvas: Canvas = Canvas()
    private var paint: Paint = Paint()
    private lateinit var myBitmap: Bitmap
    private var isAnalyzing = AtomicBoolean(false)

    var imageConverter: Runnable ?= null

    ////////////////
    var yRowStride = 0
    var yuvBytes: Array<ByteArray> = Array<ByteArray>(3, init = { ByteArray(0) })
    var rgbBytes: IntArray ?= null


    private var isFaceEmbedding = AtomicBoolean(false)



    init {
        paint.strokeWidth = 5.0f
        paint.color = Color.GREEN
        paint.style = Paint.Style.STROKE

    }

    companion object {
        const val TAG = "FACE_DETECTOR"
    }

    var analysisSizeListener: ( (Size) -> Unit) ?= null
    var faceEmbeddingListener: ( (Array<Float>) -> Unit) ?= null

    //High accuracy, all landmarks, and both smile and eye-open classification
    val highAccuracyFaceDetectorOpts = FirebaseVisionFaceDetectorOptions.Builder()
        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
        .build()

    private val realtimeOpts = FirebaseVisionFaceDetectorOptions.Builder()
//        .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
        .build()
    private val detector = firebaseVision.getVisionFaceDetector(realtimeOpts)


    interface FaceBoxListener {
        fun onFaceDetected(faces: List<FirebaseVisionFace>)
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    fun Image.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }


    fun image_to_bitmap(image:Image): Bitmap{
        /* Convert YUV_420_888 to bitmap */
        val planes = image.planes
        fillBytes(planes, yuvBytes)
        yRowStride = planes[0].rowStride

        val uvRowStride = planes[1].rowStride
        val uvPixelStride = planes[1].pixelStride
        imageConverter = Runnable {
            ImageUtils.convertYUV420ToARGB8888(
                yuvBytes[0],
                yuvBytes[1],
                yuvBytes[2],
                image.width,
                image.height,
                yRowStride,
                uvRowStride,
                uvPixelStride,
                rgbBytes!!
            )
        }
        val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(rgbBytes, 0, image.width, 0, 0, image.width, image.height)
        return bitmap
    }

    protected fun fillBytes(
        planes: Array<Plane>,
        yuvBytes: Array<ByteArray>
    ) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (i in planes.indices) {
            Log.d("test", "index: $i")
            val buffer = planes[i].buffer
            if (yuvBytes[i]!!.isEmpty() ) {
                Log.d("test", "yuvbytes is empty")
                Log.d("test", "buffer remaining: ${buffer.remaining()}")
                Log.d("test", "buffer capacity: ${buffer.capacity()}")
                yuvBytes[i] = ByteArray(buffer.capacity())
            }
//            buffer[yuvBytes[i]]
            Log.d("test", "image size: widthxhegiht ${image?.width} x ${image?.height}")
            Log.d("test", "buffer remaining: ${buffer.remaining()}")
            Log.d("test", "yuvbytes size: ${yuvBytes[i].size}")
            Log.i("TAG", "Reset")
            if(buffer.remaining() == 0){
                buffer.get(yuvBytes[i], -1 * buffer.capacity(), buffer.capacity())
            }else{
                buffer.get(yuvBytes[i])
            }
        }
    }


    private fun ImageToBitmap(image: Image): Bitmap? {
        val var2: Array<Plane> = image.planes
        Log.d("test", image?.format.toString())
        if (image.format == 256) {
            if (var2 != null && var2.size == 1) {
                var var3: ByteBuffer
                val var4 = ByteArray(var2[0].buffer.also { var3 = it }.remaining())
                var3.get(var4)
                Log.d("test", "convert")
                return BitmapFactory.decodeByteArray(var4, 0, var4.size)
            }
        }
        return null
    }

    fun addRequest(request: JsonObjectRequest){
        activityContext.get()?.requestQueue?.add(request)
        Log.i(TAG, "Adding request to queue")
    }

    private fun createFaceCompareRequest(face_embedding: FloatArray):JsonObjectRequest{
        Log.i(TAG, "Create request")

        var body = JSONObject()
        body.put("face_embedding", JSONArray(face_embedding))
//        Log.d(TAG, body.toString())
        val url = activityContext.get()?.faceCompareUrl
        val listener = Response.Listener<JSONObject> {
            Log.d(TAG, "Getting result")
           try{
               val faces = it.getJSONArray("faces")
               Log.d(TAG, "Faces match: ${faces.length()}")
               if (faces.length() > 0 ){
                   val face = faces.get(0) as JSONObject
                   val firstName = face.getString("first_name")
                   val lastName = face.getString("last_name")
                   val imageUrl = face.getString("image_url")
                   val distance = face.getString("distance")
                   Log.d(TAG, "firstname: $firstName")
                   Log.d(TAG, "lastname: $lastName")
                   Log.d(TAG, "image url: $imageUrl")
                   Log.d(TAG, "diatanec: $distance")
                   activityContext?.get()?.showToast("$firstName $lastName", Toast.LENGTH_SHORT)
               }else{
                   Log.d(TAG, "Face not match")
               }
           }catch (e : JSONException ){
                Log.d(TAG, e.message)
           }
        }
        val errorListener = Response.ErrorListener {
            try{
                var responseBody = String(it.networkResponse.data)
                var json = JSONObject(responseBody)
                Log.e(TAG, "Error message: " +  json.optString("message")                        )
                Log.e(TAG, it.toString())
            }catch (e: NullPointerException){

            }

        }
        var request = JsonObjectRequest(Request.Method.POST, url, body, listener, errorListener )
        return request
    }


    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (isAnalyzing.get()) return
        image = imageProxy.image ?: return
        isAnalyzing.set(true)

//        var rotation = activityContext.get()?.windowManager?.defaultDisplay?.rotation!!
        var rotation = getRotationCompensation(activityContext.get()?.current_camer_id!! ,activityContext.get() as Activity, activityContext.get() as Context)

        val visionImage = FirebaseVisionImage.fromMediaImage(
            image!!,
            rotation
        );
//        Log.d("test", "image buffer before passing  ${image!!.planes[0].buffer.remaining()}")


        Log.d("autofit", "rotation: $rotation")

        val bitmap = visionImage.bitmap
        var croppedBitmap: Bitmap ?= null
        var boxHeight:Int ?= null
        var boxWidth:Int ?= null
        var boxX:Int ?= null
        var boxY:Int ?= null

        detector.detectInImage(visionImage)
                .addOnSuccessListener {

                    analysisSizeListener?.invoke( Size( image!!.width, image!!.height))

                    var box:Rect ?= null

                    for( face in it){
                        Log.d(TAG, "Id : " + face.trackingId.toString())

                        box = face.boundingBox
                        var arr = IntArray(box.width() * box.height())


                        boxX = if (box.left > 0) box.left else 0  /** if x1 < 0 */
                        boxY = if(box.top > 0) box.top else 0 /**if y1 < 0 */
                        boxWidth = if(box.width() + boxX!! <= bitmap!!.width) box.width() else (bitmap!!.width - boxX!!) /** if x2 > width */
                        boxHeight = if(  box.height() + boxY!! <= bitmap!!.height) box.height() else ( bitmap!!.height - boxY!!) /**if y2 > height */

                        bitmap?.getPixels( arr, 0, box.width(),
                                            boxX!! ,
                                            boxY!! ,
                                            boxWidth!!,
                                            boxHeight!! )

                        Log.i(TAG, "Set pixel to new cropped bitmap.")
                        croppedBitmap = Bitmap.createBitmap(boxWidth!!, boxHeight!!, Bitmap.Config.ARGB_8888)
                        croppedBitmap?.setPixels(arr, 0, boxWidth!!, 0,0, boxWidth!!, boxHeight!!)
                        Log.d("Test", "Pixel" + croppedBitmap?.getPixel(10,10).toString())
                        //TODOs: add handler
                        if( !isFaceEmbedding.get()){
                            activityContext.get()!!.runInBackground(Runnable {
                                Log.d(TAG, "Face processing in background")
                                isFaceEmbedding.set(true)
                                var faceEembedding = faceEmbedding.processFace(croppedBitmap!!, rotation )

                                val request = createFaceCompareRequest(faceEembedding!! )
                                addRequest(request)
                                isFaceEmbedding.set(false)

                            })
                        }else{
                            Log.d("TAG", "Processing face; Skip frame.")
                        }
                    }



                    Log.i(TAG, "Face found: " + it.size.toString())

                    isAnalyzing.set(false)
                    faceBoxListener.onFaceDetected(it)
                    imageProxy.close()
                }.addOnFailureListener {
                    isAnalyzing.set(false)
                    Toast.makeText(activityContext.get(), "No face detected.", Toast.LENGTH_SHORT)
                        .show()
                    Log.e(TAG, "No face found.")
                    imageProxy.close()
                }
    }


}


