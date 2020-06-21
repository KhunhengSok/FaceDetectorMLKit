package com.example.facedetectormlkit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.facedetectormlkit.FaceDetectorImageAnalyzer.FaceBoxListener
import com.example.facedetectormlkit.frame.FaceBox
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (luma: Double) -> Unit

//TODOs: Add if statement to prevent multiple image on takephoto button
class MainActivity : AppCompatActivity(), CameraXConfig.Provider {

    companion object {
        private const val TAG = "MainActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSION = 10
        private var REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)

        //minumum 100x100px for face detection, 200x200px for contour detection
        val faceAnalyzeDimen = Size(700, 700)

        //camera choose longer value as width and shorter value for height
        val previewDimen = Size(480, 720)
    }

    //create createclassifer function
    //create processimage function to getface embedding
    //optimze pause, resume, stop function


    private var preview: Preview? = null

    private var imagecapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    var camera: Camera? = null
    private var videoCapture: VideoCapture? = null
    private var cameraSelector: CameraSelector? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var faceDetectorAnalyzer: ImageAnalysis? = null

    private var cachedAnalysisDemens = Size(0, 0)
    private lateinit var cachedTargetDimens: Size

    private var faceEmbedding:FaceEmbedding ?= null

    lateinit var current_camer_id: String

    private var handlerThread: HandlerThread ?= null
    private var handler:Handler ?= null
    var requestQueue:RequestQueue ?= null


    var mStorageRef = FirebaseStorage.getInstance().getReference("image/")

    public val faceCompareUrl = "http://192.168.1.60:5000/api/face_embedding/compare"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION)
        }

        camera_capture_button.setOnClickListener(View.OnClickListener {
            takePhoto()
        })
//        startConnect()
    }

    private fun init(){
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        faceEmbedding = FaceEmbedding.create(this as Activity)

        handlerThread = HandlerThread("Face Embedding")
        handlerThread?.start()
        handler = Handler( handlerThread?.looper)
        requestQueue = Volley.newRequestQueue(this)
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }

    private fun startConnect(){
        val emulator_url = "10.0.0.15:8000"
        val device_url = "http://192.168.1.60:5000/api/face_embedding/compare"

        val jsonBody = JSONObject()
//        var arr = FloatArray(2048, init= {i -> (-1..2).random().toFloat()})
        jsonBody.put("face_embedding", "Hello world")
        val request = JsonObjectRequest(Request.Method.POST, device_url, jsonBody,
                    Response.Listener{
                        Log.d("Request", it.toString())
                    },  Response.ErrorListener{
                        var responseBody = String(it.networkResponse.data)
                        var json = JSONObject(responseBody)
                        Log.e("Request", "Error message: " +  json.optString("message")                        )
                        Log.e("Request", it.toString())
                    })
        requestQueue?.add(request)
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            grantResults.apply {
                for (i in 1..this.size) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(REQUIRED_PERMISSIONS[i - 1]),
                        REQUEST_CODE_PERMISSION
                    )
                }
            }
        }
    }

    private fun getFileExtension(uri: Uri): String? {
        val cR: ContentResolver = contentResolver
        val mime: MimeTypeMap = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(cR.getType(uri))
    }

    fun uploadImage(uri:Uri, personName:String){
        val personDirRef = mStorageRef.child("$personName/")
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        val contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        var metadata = StorageMetadata.Builder().setContentType(contentType).build()
        Log.d(TAG, "uploadImage: File Content type is: $contentType")

        val fileRef = personDirRef.child(uri.lastPathSegment!!)
        if(uri.lastPathSegment != null){
            Log.d(TAG, "uploadImage: Last path segment: ${uri.lastPathSegment}")
            fileRef.putFile(uri, metadata).addOnSuccessListener {
                showToast("Success uploaded.", Toast.LENGTH_SHORT)
                Log.i(TAG, "Image upload successfully")

                fileRef.downloadUrl.addOnSuccessListener {
                    Log.d(TAG, "uploadImage: ${it.toString()}")
                    //ToDos:
                }
            }.addOnFailureListener{
                showToast("Failing to upload. ${it.message}", Toast.LENGTH_SHORT)
                Log.e(TAG, "uploadImage: $it.message" )
            }.addOnProgressListener {
                var percentage = (it.bytesTransferred / it.totalByteCount * 100 ).toInt()

            }
        }


    }

    @SuppressLint("RestrictedApi", "UnsafeExperimentalUsageError")
    private fun startCamera() {
        val processCameraProvider = ProcessCameraProvider.getInstance(this)
        val displayMatrix = DisplayMetrics().also {
            preview_view?.display?.getRealMetrics(it)
        }
        Log.d(TAG, "display matrix:$displayMatrix")

        val windowSize = Size( this.windowManager.defaultDisplay.width,
            this.windowManager.defaultDisplay.height)
        Log.d(TAG, "Window size: $windowSize")

//        preview = Preview.Builder().setTargetAspectRatioCustom(Rational(displayMatrix.widthPixels, displayMatrix.heightPixels)).build()
        try{
            preview = Preview.Builder().setTargetResolution( windowSize).build()
        }catch (e: Exception){
            Log.e(TAG, e.message)
            preview = Preview.Builder().build()
        }
        Log.d(TAG, displayMatrix.toString())

        faceDetectorAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(faceAnalyzeDimen)
            .build().also { it ->
                it.setAnalyzer(cameraExecutor, FaceDetectorImageAnalyzer(WeakReference(this), faceEmbedding!!,
                    object : FaceBoxListener {
                        override fun onFaceDetected(faces: List<FirebaseVisionFace>) {
                            facebox.faces = faces
                        }
                    }).apply {
                    this.analysisSizeListener = { size ->
                        updateOverlayTransform(facebox, size)
                    }
                    this.faceEmbeddingListener = {
                        faceEmbedding: Array<Float> ->
                            Log.d("Face Embedding", "Get face embedding")
                            Log.d("Face Embedding", "Size:  $faceEmbedding.size")
                    }

                })
            }


        cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        imagecapture =
            ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
//        videoCapture = VideoCaptureConfig.Builder().apply {
//                setTargetAspectRatio(AspectRatio.RATIO_16_9)
//            }.build()

        processCameraProvider.addListener(Runnable {
            val cameraProvider = processCameraProvider.get()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector!!, preview, imagecapture, faceDetectorAnalyzer
                )
                preview?.setSurfaceProvider(preview_view!!.createSurfaceProvider(camera?.cameraInfo))

                current_camer_id = Camera2CameraInfo.extractCameraId(camera?.cameraInfo!!)
                Log.d("autofit", "camera id: $current_camer_id")
            } catch (exe: Exception) {
                Log.e(TAG, "Use case binding failed: " + exe.message.toString())
            }
        }, ContextCompat.getMainExecutor(this))
        Log.d(TAG, "Preview view: ${preview_view.width} $preview_view.height")

    }

    override fun onPause() {
        super.onPause()

    }

    private fun takePhoto() {
        val imagecapture = imagecapture ?: return

        val personName = "-"
        var photoFile = File(
            outputDirectory,
            personName + SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()

      /*  imagecapture.takePicture(ContextCompat.getMainExecutor(this),
            object: ImageCapture.OnImageCapturedCallback(){
                override fun onCaptureSuccess(image: ImageProxy) {
                    image
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                }
            }
                )*/
        imagecapture.takePicture(
            outputOption,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    var savedUri = Uri.fromFile(photoFile)
//                    Toast.makeText(
//                        applicationContext,
//                        "Photo Captured Success. Saved at $savedUri",
//                        Toast.LENGTH_LONG
//                    ).show()
                    uploadImage(savedUri, "Khunheng Sok")

                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo captured failed.", exception)
                }
            }
        )
    }

    public fun create(){

    }

    public fun  showToast(message: String, length: Int){
        Toast.makeText(this, message, length).show()
    }

    public fun runInBackground(runnable: Runnable){
        if (handler != null){
            handler?.post(runnable)
        }
    }


    /*
     * Copyright 2019 Google LLC
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *     https://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */

    /*
     * Original source location:
     * https://github.com/android/camera/blob/master/CameraXBasic/app/src/main/java/com/android/example/cameraxbasic/utils/AutoFitPreviewBuilder.kt
     *
     * Modifications Copyright 2019 Big Nerd Ranch
    */


    private fun updateOverlayTransform(overlayView: FaceBox?, size: Size) {
        if (overlayView == null) return

        cachedTargetDimens = Size(preview_view.width, preview_view.height)

        if (size == cachedAnalysisDemens) {
            // nothing has changed since the last update, so return early
            return
        } else {
            cachedAnalysisDemens = size
        }


        Log.d("autofit", "cachedAnalysisDimens are now $cachedAnalysisDemens")
        Log.d("autofit", "cachedTargetDimens are now $cachedTargetDimens")
        Log.d("autofit", "viewFinderDimens are now $cachedAnalysisDemens")

        overlayView.transform = overlayMatrix()
    }

    private fun overlayMatrix(): Matrix {
        val matrix = Matrix()

        // ---- SCALE the overlay to match the preview ----
        // Buffers are roated relative to the device's 'natural' orientation: swap width and height
        val scale = cachedTargetDimens.height.toFloat() / cachedAnalysisDemens.width.toFloat()
        Log.d("autofit", "Scale is $scale")

        // Scale input buffers to fill the view finder
        matrix.preScale(scale, scale)

        // ---- MOVE the overlay ----
        // move all the points of the overlay so that the relative (0,0) point is at the top-left of the preview
        var xTranslate: Float
        var yTranslate: Float
        if (preview_view?.width!! > preview_view?.height!!) {
            // portrait: viewFinder width corresponds to target height
            Log.d("Autofit", "portrait")
            xTranslate = (preview_view?.width!! - cachedTargetDimens?.height) / 2f
            yTranslate = (preview_view?.height!! - cachedTargetDimens?.width) / 2f
        } else {
            // landscape: viewFinder width corresponds to target width
            Log.d("Autofit", "landscape")
            xTranslate = (preview_view?.width!! - cachedTargetDimens?.width) / 2f
            yTranslate = (preview_view?.height!! - cachedTargetDimens?.height) / 2f
        }
        /* xTranslate = (preview_view?.width!! - cachedAnalysisDemens?.height) /2f
         yTranslate = (preview_view?.height!! - cachedAnalysisDemens?.width) /2f*/

        xTranslate -= 288
//        yTranslate -=  352
        matrix.postTranslate(xTranslate, yTranslate)
        Log.d("Autofit", "Translatex " + xTranslate.toString())
        Log.d("Autofit", "Translatey " + yTranslate.toString())

        Log.d("Autofit", "Preview_view: ${preview_view.width}")
        Log.d("Autofit", "Preview_view: ${preview_view.height}")
        Log.d("Autofit", "CachedTargetDimen: ${cachedTargetDimens.width}")
        Log.d("Autofit", "CachedTargetDimen: ${cachedTargetDimens.height}")
        Log.d("Autofit", "CachedAnalysisDimen: ${cachedAnalysisDemens.width}")
        Log.d("Autofit", "CachedAnalysisDimen: ${cachedAnalysisDemens.height}")

        // ---- MIRROR the overlay ----
        // Compute the center of the view finder
        val centerX = preview_view?.width!! / 2f
        val centerY = preview_view?.height!! / 2f
        matrix.postScale(-1f, 1f, centerX, centerY)


        Log.d("Autofit", "Matrix: $matrix")
        return matrix
    }


}
