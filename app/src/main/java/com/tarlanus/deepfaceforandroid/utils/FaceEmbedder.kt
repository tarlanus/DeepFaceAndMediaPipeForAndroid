package com.tarlanus.deepfaceforandroid.utils

import android.content.Context
import android.graphics.Bitmap
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import kotlin.math.sqrt

class FaceEmbedder(context: Context) {
    private val interpreter: Interpreter

    init {
        interpreter = Interpreter(loadModelFile(context, "deepface.tflite"))
    }
    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelName)
        val inputStream = assetFileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    fun getEmbedding(bitmap: Bitmap): FloatArray {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(160, 160, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(127.5f, 127.5f))
            .build()

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        val processed = imageProcessor.process(tensorImage)

        val inputBuffer = processed.buffer
        val output = Array(1) { FloatArray(128) }

        interpreter.run(inputBuffer, output)
        return output[0]
    }
    fun l2Normalize(emb: FloatArray): FloatArray {
        var sum = 0f
        for (v in emb) sum += v * v
        val norm = kotlin.math.sqrt(sum)
        return FloatArray(emb.size) { i -> emb[i] / norm }
    }
    fun cosineSimilarity(emb1: FloatArray, emb2: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in emb1.indices) {
            dot += emb1[i] * emb2[i]
            normA += emb1[i] * emb1[i]
            normB += emb2[i] * emb2[i]
        }
        return dot / (sqrt(normA) * sqrt(normB))
    }


}