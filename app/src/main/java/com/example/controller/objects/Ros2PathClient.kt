package com.example.controller.objects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import org.json.JSONObject

object Ros2PathClient : Ros2Client() {

    data class Pose2D(val x: Double, val y: Double)

    private val pathPoints = mutableListOf<Pose2D>()

    fun subscribeToPathTopic() {
        try {
            subscribe("/plan", "nav_msgs/msg/Path")
        } catch (e: Exception) {
            Log.e("Ros2PathClient", "Failed to subscribe to path topic", e)
        }
    }

    fun startStream() {
        SharedWebSocketClient.pathHandler = {
            parsePath(it)
        }
        subscribeToPathTopic()
    }

    private fun parsePath(msg: JSONObject?) {
        msg?.let {
            try {
                pathPoints.clear()
                val posesArray = msg.optJSONArray("poses") ?: return

                for (i in 0 until posesArray.length()) {
                    val poseObj = posesArray.getJSONObject(i).optJSONObject("pose") ?: continue
                    val position = poseObj.optJSONObject("position") ?: continue

                    val x = position.optDouble("x", 0.0)
                    val y = position.optDouble("y", 0.0)

                    pathPoints.add(Pose2D(x, y))
                }
            } catch (e: Exception) {
                Log.e("Ros2PathClient", "Error parsing path message", e)
            }
        }
    }

    fun overlayPath(mapBitmap: Bitmap, mapInfo: Ros2MapClient.MapMetaData): Bitmap? {
        try {
            val output = mapBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(output)

            val paint = Paint().apply {
                color = Color.GREEN
                strokeWidth = 3f
                isAntiAlias = true
            }

            val resolution = mapInfo.resolution
            val originX = mapInfo.originX
            val originY = mapInfo.originY
            val height = mapBitmap.height

            for (i in 1 until pathPoints.size) {
                val p1 = pathPoints[i - 1]
                val p2 = pathPoints[i]

                val px1 = ((p1.x - originX) / resolution).toFloat()
                val py1 = (height - 1) - ((p1.y - originY) / resolution).toFloat()

                val px2 = ((p2.x - originX) / resolution).toFloat()
                val py2 = (height - 1) - ((p2.y - originY) / resolution).toFloat()

                canvas.drawLine(px1, py1, px2, py2, paint)
            }

            return output
        } catch (e: Exception) {
            Log.e("Ros2PathClient", "Error overlaying path", e)
            return null
        }
    }
}