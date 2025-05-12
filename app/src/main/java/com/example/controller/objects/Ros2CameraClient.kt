package com.example.controller.objects

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import org.json.JSONObject

object Ros2CameraClient: Ros2Client() {
    //private val sharedWebSocketClient = SharedWebSocketClient.getInstance()
    var imageListener: ((Bitmap) -> Unit)? = null

    fun startStream() {
        println("Connected to ROS2 WebSocket")
        SharedWebSocketClient.cameraImgHandler={
            processJsonImg(it)
        }
        subscribeToCompressedImage()
    }

    private fun subscribeToCompressedImage() {
        try {
//            val subscribeMsg = JSONObject().apply {
//                put("op", "subscribe")
//                put("topic", "/usb_cam/image_raw/compressed")
//                put("type", "sensor_msgs/msg/CompressedImage")
//            }
//            SharedWebSocketClient.getInstance().send(subscribeMsg.toString())
            subscribe("/camera/image_raw/compressed", "sensor_msgs/msg/CompressedImage")
        } catch (e: Exception) {
            Log.e("Ros2CameraClient", "Failed to subscribe to compressed image topic", e)
        }
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            //e.printStackTrace()
            Log.e("Ros2CameraClient", "Error processing camera image", e)
            null
        }
    }

    private fun processJsonImg(data: JSONObject?)
    {
        data ?: return
        try {
            val imageData = data.optString("data")
            val bitmap = decodeBase64ToBitmap(imageData)
            bitmap?.let {
                imageListener?.invoke(it)
            }
        } catch (e: Exception) {
            Log.e("Ros2CameraClient", "Error processing camera image", e)
        }
    }

}