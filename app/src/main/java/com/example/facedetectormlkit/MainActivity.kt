package com.example.facedetectormlkit

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.Image
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.*
import androidx.camera.core.impl.VideoCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.lang.Exception
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.core.ImageAnalysis
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark
import java.lang.ref.WeakReference

typealias LumaListener = ( luma: Double) ->Unit


class MainActivity: AppCompatActivity(), CameraXConfig.Provider {
    private var preview:Preview ?= null
    private var previewView:PreviewView ?= null

    private var imagecapture: ImageCapture ?= null
    private var imageAnalyzer: ImageAnalysis ?= null
    private var camera: Camera?= null
    private var videoCapture: VideoCapture ?= null
    private var cameraSelector: CameraSelector ?= null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor:ExecutorService
    private lateinit var cameraXConfig: CameraXConfig;

//    private var faceDetectorAnalyzer: com.example.facedetectormlkit.FaceDetectorImageAnalyzer ?= null
    private var faceDetectorAnalyzer: ImageAnalysis  ?= null



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()){
            startCamera()
        }else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION  )
        }

        camera_capture_button.setOnClickListener( View.OnClickListener {
            takePhoto()
        })

    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let{
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all{
        ContextCompat.checkSelfPermission(baseContext, it ) == PackageManager.PERMISSION_GRANTED
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSION){
            if(allPermissionsGranted()){
                startCamera()
            }else{
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }else{
            grantResults.apply {
                for (i in 1..this.size){
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(REQUIRED_PERMISSIONS[i - 1]), REQUEST_CODE_PERMISSION)
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera(){
        lateinit var myBitmap:Bitmap
        var canvas:Canvas ?= null
        val paint = Paint().apply {
            strokeWidth = 5.0f
            color = Color.GREEN
            style = Paint.Style.STROKE
        }

        val processCameraProvider = ProcessCameraProvider.getInstance(this)
        preview = Preview.Builder().setTargetResolution(
            Size(720, 1080)
        ).build()


        imageAnalyzer = ImageAnalysis.Builder().build().also {
            it.setAnalyzer(cameraExecutor, LuminosityImageAnalyzer {
                    luma -> Log.i(TAG, "Average luminosity: $luma") //interface
            })
        }

        faceDetectorAnalyzer = ImageAnalysis.Builder().build().also {
            it.setAnalyzer(cameraExecutor, FaceDetectorImageAnalyzer (WeakReference(this)))
        }

        cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
        imagecapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
        videoCapture = VideoCaptureConfig.Builder().apply {
                setTargetAspectRatio(AspectRatio.RATIO_16_9)
            }.build()

        processCameraProvider.addListener( Runnable {
            val cameraProvider = processCameraProvider.get()

            try{
                cameraProvider.unbindAll()
                //TODO: add image analysis
                camera = cameraProvider.bindToLifecycle(this,
                    cameraSelector!!, preview, imagecapture,  faceDetectorAnalyzer)
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider(camera?.cameraInfo))

            }catch (exe: Exception){
                Log.e(TAG, "Use case binding failed" ,exe)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onPause() {
        super.onPause()

    }

    private fun takePhoto(){
        val imagecapture = imagecapture ?: return

        var photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imagecapture.takePicture( outputOption, ContextCompat.getMainExecutor(this),  object: ImageCapture.OnImageSavedCallback{
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                var savedUri = Uri.fromFile(photoFile)
                Toast.makeText(applicationContext, "Photo Captured Sucess. Saved at " + savedUri, Toast.LENGTH_LONG).show()
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Photo captured failed.", exception)
            }
        })
    }



    companion object{
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSION = 10
        private var REQUIRED_PERMISSIONS = arrayOf( android.Manifest.permission.CAMERA)
    }


    private class LuminosityImageAnalyzer(private val listener: LumaListener): ImageAnalysis.Analyzer{
        private fun ByteBuffer.toByteArray(): ByteArray{
            rewind()
            val data = ByteArray(remaining())
            get(data)
            return data
        }

        override fun analyze(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map{ it.toInt() and 0xFF}
            val luma = pixels.average()

            listener(luma)
            image.close()

        }
    }


}
