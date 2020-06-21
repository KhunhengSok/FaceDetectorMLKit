package com.example.facedetectormlkit.frame

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.graphics.toRectF
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import kotlinx.android.synthetic.main.activity_main.view.*

class FaceBox @JvmOverloads constructor(
    context: Context ?= null,
    attrs: AttributeSet ?= null,
    defStyleAttr: Int = - 1
    ): View(context, attrs, defStyleAttr){

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 3.0f
    }

    //raw faces
    var faces = listOf<FirebaseVisionFace>()
        set(value){
            field = value
            transformBox()
            invalidate()
        }

    //transform faces
    var drawingFaces = listOf<RectF>()

    var transform = Matrix()
        set(value) {
            field = value
            transformBox()
        }

    private fun transformBox(){
        //build src and st

        //for points
        /* var transformInput = faces.flatMap{
            listOf(it.boundingBox.left.toFloat(),
                it.boundingBox.top.toFloat(), it.boundingBox.right.toFloat(), it.boundingBox.bottom.toFloat()
            )
        }.toFloatArray()

        drawingFaces = transformOutput.asList().chunked(
            size=4, transform = { (l, t, r, b) -> RectF(l, t, r, b) }
        )
    }
        */

        var transformInput = faces.flatMap {
            listOf( RectF(it.boundingBox) )
        }
//        var transformOutput:Array<RectF?> = arrayOfNulls<RectF?>(transformInput.size)
        var transformOutput = arrayListOf<RectF>()

        if (transformInput.isNotEmpty()){
            for(element in transformInput){
//            transform.mapRect(transformOutput[i], transformInput[i])
                var temp =  RectF()
//                transform.mapRect(temp, element)

                transform.mapRect(temp, element)
                transformOutput.add(temp)


                /*var m =  Matrix()
                m.setScale(2.0f , 2f)
                m.mapRect(element)
                Log.d("autofit", m.toString())
                Log.d("autofit", element.toString())
*/
            }
        }

//        drawingFaces = transformOutput.asList().chunked(
//            size=4, transform = { (l, t, r, b) -> RectF(l, t, r, b) }
//        )
        drawingFaces = transformOutput
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
//            old
            /*for( face in faces){
                var box = face.boundingBox
                drawRoundRect(RectF(box), 0.0f, 0.0f, paint)
                Log.i(TAG, "Drew")
            }*/

//            Log.d("autofit", faces.firstOrNull()?.boundingBox.toString())
//            Log.d("autofit", transform.toString())
//            Log.d("autofit", drawingFaces.toString())

            for ( face in drawingFaces){
                drawRect(face, paint)
                Log.i(TAG, "Drew")
            }

        }
    }


    companion  object{
        var TAG = "Face Box"
    }
}