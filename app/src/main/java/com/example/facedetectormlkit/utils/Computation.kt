package com.example.facedetectormlkit.utils

import kotlin.math.pow

fun dot(v1: FloatArray, v2: FloatArray): Float {
    if (v1.size != v2.size) return -1f

    var result: Float = 0f
    for (i in v1.indices) {
        result += v1[i] * v2[i]
    }
        return result
}

fun norm(v: FloatArray): Float{
    var normValue = 0.0
    for (ele in v){
        normValue += (ele as Double).pow(2.0)
    }
    return normValue as Float
}

fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float{
    var result = 0f
    var normV1 = norm(v1)
    var normV2 = norm(v2)
    var dotProduct = dot(v1, v2)
    return (dotProduct / (normV1 * normV2))
}