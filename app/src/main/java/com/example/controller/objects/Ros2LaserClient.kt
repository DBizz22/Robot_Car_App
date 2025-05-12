package com.example.controller.objects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.sin

object Ros2LaserClient:Ros2Client() {
    //private val sharedWebSocketClient = SharedWebSocketClient.getInstance()
    private var warning = 0
    private var angleMin: Double = 0.0
    private var angleMax: Double = 0.0
    private var angleIncrement: Double = 0.0
    private var rangeMax: Double = 0.0
    private var rangeMin: Double = 0.0
    private var intensitiesJson = JSONArray()
    private var rangesJson = JSONArray()
    //var laserScanListener: ((Bitmap) -> Unit)? = null

    fun startStream() {
        println("Connected to ROS2 WebSocket")
        SharedWebSocketClient.laserScanHandler={
            parseLaserScanJSON(it)
        }
        subscribeToScanTopic()
    }

    private fun subscribeToScanTopic() {
        try {
            subscribe("/scan","sensor_msgs/msg/LaserScan")
        } catch (e: Exception) {
            Log.e("ROS2LaserClient", "Failed to subscribe to /scan topic", e)
        }
    }

    private fun parseLaserScanJSON(msg: JSONObject?){
        msg ?: return
        try {
            rangesJson = msg.optJSONArray("ranges") ?: return
            intensitiesJson = msg.optJSONArray("intensities") ?: return
            angleMin = msg.optDouble("angle_min", 0.0)
            angleMax = msg.optDouble("angle_min", 0.0)
            angleIncrement = msg.optDouble("angle_increment", 0.0)
            rangeMax = msg.optDouble("range_max", 10.0)
            rangeMin = msg.optDouble("range_min", 0.1)
        } catch (e: Exception) {
            Log.e("ROS2LaserClient", "Error processing laser scan data", e)
        }
    }

    fun overlayLaserScanData(mapBitmap: Bitmap, mapInfo: Ros2MapClient.MapMetaData, robotPose: Ros2PoseClient.Pose2D): Bitmap?{
        try {
            val overlayBitmap = mapBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(overlayBitmap)
            val paint = Paint().apply {
                strokeWidth = 2f
            }

            val resolution = mapInfo.resolution
            val originX = mapInfo.originX
            val originY = mapInfo.originY

            val width = mapBitmap.width
            val height = mapBitmap.height
            warning = 0

            for (i in 0 until rangesJson.length()) {
                val range = rangesJson.optDouble(i, Double.NaN)
                if (range.isNaN() || range < rangeMin || range > rangeMax) continue

                val angle = angleMin + i * angleIncrement
                val laserX = range * cos(angle)
                val laserY = range * sin(angle)

                // Transform from robot-local to map-global coordinates
                val mapX = robotPose.x + laserX * cos(robotPose.theta) - laserY * sin(robotPose.theta)
                val mapY = robotPose.y + laserX * sin(robotPose.theta) + laserY * cos(robotPose.theta)

                // Convert map coords to pixels
                val px = ((mapX - originX) / resolution).toFloat()
                val py = height - 1 - ((mapY - originY) / resolution).toFloat() // Flip Y-axis

                //if (px in 0 until width && py in 0 until height)
                if(px in 0f..width.toFloat() && py in 0f..height.toFloat()){
                    val intensity = intensitiesJson?.optDouble(i, 0.0) ?: 0.0
                    val normalized = (intensity / 255.0).coerceIn(0.0, 1.0)
                    paint.color = Color.rgb((normalized * 255).toInt(), 0, (255 - normalized * 255).toInt())
                    canvas.drawCircle(px.toFloat(), py.toFloat(), 2f, paint)
                }
            }

            //laserScanListener?.invoke(overlayBitmap)
            return overlayBitmap
        } catch (e: Exception) {
            Log.e("ROS2LaserClient", "Error processing laser scan data", e)
            return null
        }
    }
}