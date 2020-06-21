package com.example.facedetectormlkit

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import com.android.volley.toolbox.Volley
import com.example.facedetectormlkit.utils.cosineSimilarity
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.MappedByteBuffer

class FaceEmbedding {
//    private var faceEmbedding: FloatArray ?= null

    private var inputImage:TensorImage ?= null
//    private var output = TensorBuffer.createFixedSize( intArrayOf( 1, 1024), DataType.FLOAT32)
    private var output:TensorBuffer ?= null

    private var rotation = 0

    //Static value
    // for quantization model
    private val IMAGE_MEAN = 0.0f
    private val IMAGE_STD = 1.0f
    private val IMAGE_WIDTH = 224
    private val IMAGE_HEIGHT  = 224

    //tensorflow lite
    private var tfliteInterpreter:Interpreter ?= null
    private var tfliteOpts = Interpreter.Options()
//    private val postProcessor:TensorProcessor ?= null
    private var tfliteModel: MappedByteBuffer ?= null
    private var imageProcessor:ImageProcessor ?= null

    private val TAG = "FACE_EMBEDDING"

    companion object {
        var faceEmbedding:FaceEmbedding ?= null

        fun create(activity: Activity):FaceEmbedding{
            if(faceEmbedding == null){
                faceEmbedding = FaceEmbedding(activity)
            }
            return faceEmbedding as FaceEmbedding
        }

        fun compareFace(srcFaceEmbedding: FloatArray,
                        dstFaceEmbedding: FloatArray, min_distance:Float = 0.38f ): Boolean{
            if(srcFaceEmbedding.size != dstFaceEmbedding.size){
                return false
            }
            var distance = cosineSimilarity(srcFaceEmbedding, dstFaceEmbedding)

            return distance <= min_distance
        }
    }



    protected constructor(activity: Activity){
        tfliteModel = FileUtil.loadMappedFile(activity, getModelPath())
        try{
            tfliteOpts.addDelegate(GpuDelegate())
            Log.d(TAG, "Set GPU Delegate ")
            tfliteInterpreter = Interpreter(tfliteModel!!, tfliteOpts)
        }catch (e:Exception){
            //NNAPI support android version 8.1+
            if( Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1){
                Log.d(TAG, "Set NN api delegate")
                tfliteOpts.addDelegate(NnApiDelegate())
            }else{
                Log.d(TAG, "Set CPU delegate")
            }
            tfliteInterpreter = Interpreter(tfliteModel!!, tfliteOpts)
/*            (activity as MainActivity).apply{
                this.showToast("Cannot load model", Toast.LENGTH_LONG)
            }*/
            Log.e(TAG, e.message)
        }
        var inputDataTensorBuffer = tfliteInterpreter!!.getInputTensor(0).dataType()
        inputImage = TensorImage(inputDataTensorBuffer )
        var outputShape = tfliteInterpreter!!.getOutputTensor(0).shape() // {1, 1024}
        var outputDatatype = tfliteInterpreter!!.getOutputTensor(0).dataType()
        Log.d(TAG, "Tensorflow output shape: ${outputShape.joinToString()}" )
        Log.d(TAG, "Tensorflow output data type: ${outputDatatype}" )
        output = TensorBuffer.createFixedSize(outputShape, outputDatatype )

    }


    fun getModelPath() =  "face_embedding.tflite"


    fun getPostOps(): NormalizeOp{
        return NormalizeOp(0f, 255f)
    }

    private fun getPreprocessingNormalizeOps(): NormalizeOp{
        return NormalizeOp(IMAGE_MEAN, IMAGE_STD)
    }


    private fun loadImage(bitmap:Bitmap,  sensorOrientation: Int): TensorImage{
        inputImage?.load(bitmap)
        rotation  = sensorOrientation
        imageProcessor = ImageProcessor.Builder()
            .add( ResizeOp(IMAGE_HEIGHT, IMAGE_WIDTH, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add( ResizeWithCropOrPadOp(IMAGE_HEIGHT, IMAGE_WIDTH))
            .add( Rot90Op(rotation))
            .build()
        return imageProcessor!!.process(inputImage)
    }


    fun processFace(bitmap: Bitmap, sensorOrientation: Int): FloatArray?{
        val startTimeForLoadImage = SystemClock.uptimeMillis()
        rotation = sensorOrientation
        inputImage = loadImage(bitmap, sensorOrientation)
        val endTimeForLoadImage = SystemClock.uptimeMillis()
        Log.d(TAG, "Image load time: ${endTimeForLoadImage - startTimeForLoadImage}")


        val startTimeForFaceEmbedding = SystemClock.uptimeMillis()
        
        Log.d(TAG, "Tensorflow lite input size (width x height) : ${inputImage?.bitmap?.width} x  " +
                "${inputImage?.bitmap!!.height}"  )
        Log.d(TAG, "Tensorflow lite input image datatype: ${inputImage?.dataType}")
        Log.d(TAG, "Tensorflow lite input buffer size: ${inputImage?.buffer?.remaining()}")
        Log.d(TAG, "Tensorflow lite input buffer size: ${inputImage?.buffer?.capacity()}")
        Log.d(TAG, "Tensorflow lite output size: ${output?.buffer?.capacity()}")
        Log.d(TAG, "Tensorflow lite output datatype: ${output?.dataType}")
        Log.d(TAG, "Tensorflow lite model input datatype: " + tfliteInterpreter!!.getInputTensor(0).dataType())
        Log.d(TAG, "Tensorflow lite model input num dimens: " + tfliteInterpreter!!.getInputTensor(0).numDimensions())
        Log.d(TAG, "Tensorflow lite model input bytes: " + tfliteInterpreter!!.getInputTensor(0).numBytes())
        Log.d(TAG, "Tensorflow lite model input shape: " + tfliteInterpreter!!.getInputTensor(0).shape().joinToString())

        tfliteInterpreter?.run( inputImage!!.buffer, output?.buffer?.rewind() )

        Log.d(TAG, "Face embedding size is: ${output?.flatSize.toString()}")
        val endTimeForFaceEmbedding = SystemClock.uptimeMillis()
        Log.d(TAG, "Face processing time: ${endTimeForFaceEmbedding - startTimeForFaceEmbedding}")
//        faceEmbedding = output?.floatArray
        return output?.floatArray
    }
}