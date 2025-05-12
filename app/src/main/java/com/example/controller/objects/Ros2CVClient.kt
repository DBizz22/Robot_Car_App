package com.example.controller.objects

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.Base64
import org.json.JSONObject

object Ros2CVClient: Ros2Client() {
    var imageListener: ((Bitmap) -> Unit)? = null
    var binaryImageListener: ((Bitmap) -> Unit)? = null
    var statusListener: ((String) -> Unit)? = null

    enum class MODES(private val signalData: Int) {
        NONE(0),
        HAND_TRACKER(1),
        LINE_FOLLOWER(2);

        fun signal(): Int {
            return signalData
        }
    }

    fun startStream() {
        println("Connected to ROS2 WebSocket")
        subscribeToImage()
        subscribeToBinaryImage()
        subscribeToStatus()
        closeTrackerMode()
    }

    private fun subscribeToImage() {
        try {
            subscribe("/image", "sensor_msgs/CompressedImage")
        } catch (e: Exception) {
            Log.e("Ros2ImageClient", "Failed to subscribe to CV image topic", e)
        }
        SharedWebSocketClient.cvImgHandler={
            processCVImg(it)
        }
    }

    private fun subscribeToBinaryImage() {
        try {
            subscribe("/binary_image", "sensor_msgs/msg/CompressedImage")
        } catch (e: Exception) {
            Log.e("Ros2ImageClient", "Failed to subscribe to CV binary image topic", e)
        }
        SharedWebSocketClient.cvBinaryImgHandler={
            processCVBinaryImg(it)
        }
    }

    private fun subscribeToStatus() {
        try {
            subscribe("/status", "std_msgs/msg/String")
        } catch (e: Exception) {
            Log.e("Ros2ImageClient", "Failed to subscribe to CV binary image topic", e)
        }
        SharedWebSocketClient.cvStatusHandler={
            processCVStatus(it)
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

    private fun processJson(data: JSONObject?): Bitmap?
    {
        data ?: return null
        return try {
            val imageData = data.optString("data")
            val bitmap = decodeBase64ToBitmap(imageData)
            return bitmap
        } catch (e: Exception) {
            Log.e("Ros2ImageClient", "Error processing CV image", e)
            null
        }
    }

    private fun processCVImg(data: JSONObject?)
    {
        val bitmap=processJson(data)
        bitmap?.let {
            imageListener?.invoke(it)
        }
    }

    private fun processCVBinaryImg(data: JSONObject?)
    {
        val bitmap=processJson(data)
        bitmap?.let {
            binaryImageListener?.invoke(it)
        }
    }

    private fun  processCVStatus(data: JSONObject?){
        try {
            val msg = data?.optString("data") ?: return
            statusListener?.invoke(msg)
        }catch (e: Exception){
            Log.e("Ros2ImageClient", "Error processing CV status", e)
        }
    }

    private fun setListener(mode: MODES) {
        val data:Int = mode.signal()
        val message = JSONObject()
        message.put("op", "publish")
        message.put("topic", "/listener")

        val msg = JSONObject()
        msg.put("data", data)

        message.put("msg", msg)

        try {
            SharedWebSocketClient.getInstance().send(message.toString())
        }catch (e: Exception){
            Log.e("Ros2ImageClient", "Error Publishing to Listener", e)
        }
    }

    private fun setLineListener() {
        setListener(MODES.LINE_FOLLOWER)
    }

    private fun setHandListener() {
        setListener(MODES.HAND_TRACKER)
    }

    fun setLineFollowerEvent(value: Int){
        try {
            setParameter("follow_line","event",value)
            setLineListener()
        } catch (e: Exception) {
            Log.e("Ros2ImageClient", "Error Setting Event Parameter", e)
        }
    }

    fun setLineFollowerPixel(x: Int, y: Int){
        try {
            setParameter("follow_line","X",x)
            setParameter("follow_line","Y",y)
            setLineListener()
        } catch (e: Exception) {
            Log.e("Ros2ImageClient", "Error Setting Pixel Coords Parameter", e)
        }
    }

    fun setLineFollowerMode(){
        setLineListener()
    }

    fun setHandTrackerMode(){
        setHandListener()
    }

    fun closeTrackerMode(){
        setListener(MODES.NONE)
    }

}