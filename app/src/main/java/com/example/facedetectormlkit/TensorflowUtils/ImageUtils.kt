package com.example.facedetectormlkit.TensorflowUtils

import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.logging.Logger

/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

//package org.tensorflow.lite.examples.classification.env

/** Utility class for manipulating images.  */
object ImageUtils {
    // This value is 2 ^ 18 - 1, and is used to clamp the RGB values before their ranges
    // are normalized to eight bits.
    const val kMaxChannelValue = 262143
    private val TAG = "ImageUtils"
//    private val Log: Logger = Logger()

    /**
     * Utility method to compute the allocated size in bytes of a YUV420SP image of the given
     * dimensions.
     */
    fun getYUVByteSize(width: Int, height: Int): Int {
        // The luminance plane requires 1 byte per pixel.
        val ySize = width * height

        // The UV plane works on 2x2 blocks, so dimensions with odd size must be rounded up.
        // Each 2x2 block takes 2 bytes to encode, one each for U and V.
        val uvSize = (width + 1) / 2 * ((height + 1) / 2) * 2
        return ySize + uvSize
    }
    /**
     * Saves a Bitmap object to disk for analysis.
     *
     * @param bitmap The bitmap to save.
     * @param filename The location to save the bitmap to.
     */
    /**
     * Saves a Bitmap object to disk for analysis.
     *
     * @param bitmap The bitmap to save.
     */
    @JvmOverloads
    fun saveBitmap(bitmap: Bitmap, filename: String = "preview.png") {
        val root = Environment.getExternalStorageDirectory()
            .absolutePath + File.separator + "tensorflow"
//        Log.i(
//            "Saving %dx%d bitmap to %s.",
//            bitmap.width,
//            bitmap.height,
//            root
//        )
        val myDir = File(root)
        if (!myDir.mkdirs()) {
            Log.i(TAG, "Make dir failed")
        }
        val file = File(myDir, filename)
        if (file.exists()) {
            file.delete()
        }
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 99, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            Log.e(TAG, e.message)
        }
    }

    fun convertYUV420SPToARGB8888(
        input: ByteArray,
        width: Int,
        height: Int,
        output: IntArray
    ) {
        val frameSize = width * height
        var j = 0
        var yp = 0
        while (j < height) {
            var uvp = frameSize + (j shr 1) * width
            var u = 0
            var v = 0
            var i = 0
            while (i < width) {
                val y = 0xff and input[yp].toInt()
                if (i and 1 == 0) {
                    v = 0xff and input[uvp++].toInt()
                    u = 0xff and input[uvp++].toInt()
                }
                output[yp] = YUV2RGB(y, u, v)
                i++
                yp++
            }
            j++
        }
    }

    private fun YUV2RGB(y: Int, u: Int, v: Int): Int {
        // Adjust and check YUV values
        var y = y
        var u = u
        var v = v
        y = if (y - 16 < 0) 0 else y - 16
        u -= 128
        v -= 128

        // This is the floating point equivalent. We do the conversion in integer
        // because some Android devices do not have floating point in hardware.
        // nR = (int)(1.164 * nY + 2.018 * nU);
        // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
        // nB = (int)(1.164 * nY + 1.596 * nV);
        val y1192 = 1192 * y
        var r = y1192 + 1634 * v
        var g = y1192 - 833 * v - 400 * u
        var b = y1192 + 2066 * u

        // Clipping RGB values to be inside boundaries [ 0 , kMaxChannelValue ]
        r =
            if (r > kMaxChannelValue) kMaxChannelValue else if (r < 0) 0 else r
        g =
            if (g > kMaxChannelValue) kMaxChannelValue else if (g < 0) 0 else g
        b =
            if (b > kMaxChannelValue) kMaxChannelValue else if (b < 0) 0 else b
        return -0x1000000 or (r shl 6 and 0xff0000) or (g shr 2 and 0xff00) or (b shr 10 and 0xff)
    }

    fun convertYUV420ToARGB8888(
        yData: ByteArray,
        uData: ByteArray,
        vData: ByteArray,
        width: Int,
        height: Int,
        yRowStride: Int,
        uvRowStride: Int,
        uvPixelStride: Int,
        out: IntArray
    ) {
        var yp = 0
        for (j in 0 until height) {
            val pY = yRowStride * j
            val pUV = uvRowStride * (j shr 1)
            for (i in 0 until width) {
                val uv_offset = pUV + (i shr 1) * uvPixelStride
                out[yp++] = YUV2RGB(
                    0xff and yData[pY + i].toInt(),
                    0xff and uData[uv_offset].toInt(),
                    0xff and vData[uv_offset].toInt()
                )
            }
        }
    }
}