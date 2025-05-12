package com.example.controller.objects

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

object Ros2MapClient:Ros2Client(){
    //private val sharedWebSocketClient = SharedWebSocketClient.getInstance()
    data class MapMetaData(
        val width: Int=0,
        val height: Int=0,
        val resolution: Double=0.0,
        val originX: Double=0.0,
        val originY: Double=0.0,
        val validity : Boolean = false
    )
    var dataArray= JSONArray()
    var mapMetaData : MapMetaData = MapMetaData()
    var mapListener: ((Bitmap) -> Unit)? = null
    fun startStream() {
        println("Connected to ROS2 WebSocket")
        SharedWebSocketClient.mapHandler={
            parseMapJSON(it)
        }
        subscribeToMapTopic()
    }

    private fun subscribeToMapTopic() {
        try {
            subscribe("/map","nav_msgs/msg/OccupancyGrid")
        } catch (e: Exception) {
            Log.e("Ros2MapClient", "Failed to subscribe to compressed map topic", e)
        }
    }

    fun rotateBitmap90(source: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            postRotate(-90f) // Rotate 90 degrees clockwise
            // postRotate(-90f) // For counter-clockwise rotation
        }
        return Bitmap.createBitmap(
            source,
            0, 0,
            source.width, source.height,
            matrix,
            true // Filtering enabled
        )
    }

    private fun parseMapJSON(msg: JSONObject?){
        msg?: return
        try {
            mapMetaData=MapMetaData(
                width = msg.getJSONObject("info").getInt("width"),
                height= msg.getJSONObject("info").getInt("height"),
                resolution = msg.getJSONObject("info").getDouble("resolution"),
                originX = msg.getJSONObject("info").getJSONObject("origin").getJSONObject("position").getDouble("x"),
                originY =  msg.getJSONObject("info").getJSONObject("origin").getJSONObject("position").getDouble("y"),
                validity = true
            )
            dataArray = msg.getJSONArray("data")
        } catch (e: Exception) {
            Log.e("Ros2MapClient", "Error processing map data", e)
            mapMetaData=MapMetaData()
        }
    }

    fun overlayMap(robotPose: Ros2PoseClient.Pose2D): Bitmap?{
        try {
            val bitmap = Bitmap.createBitmap(mapMetaData.width, mapMetaData.height, Bitmap.Config.ARGB_8888)
            for (y in 0 until mapMetaData.height) {
                for (x in 0 until mapMetaData.width) {
                    val value = dataArray.getInt(y * mapMetaData.width + x)
                    val color = when(value) {
                        in 0..50 -> Color.WHITE      // Free
                        in 80..100 -> Color.BLACK           // Occupied
                        else -> Color.GRAY           // Unknown
                    }
                    bitmap.setPixel(x, mapMetaData.height - y - 1, color) // Flip Y-axis
                }
            }
            return bitmap

            // Create the raw map bitmap
//            val rawBitmap = Bitmap.createBitmap(mapMetaData.width, mapMetaData.height, Bitmap.Config.ARGB_8888)
//            for (y in 0 until mapMetaData.height) {
//                for (x in 0 until mapMetaData.width) {
//                    val value = dataArray.getInt(y * mapMetaData.width + x)
//                    val color = when (value) {
//                        in 0..50 -> Color.WHITE
//                        in 80..100 -> Color.BLACK
//                        else -> Color.GRAY
//                    }
//                    rawBitmap.setPixel(x, mapMetaData.height - y - 1, color) // Flip Y
//                }
//            }
//
//            // Create a matrix for transformation
//            val matrix = Matrix()
//
//            // Translate so that robot pose is centered on the screen (optional, if you want to center robot)
////            val scale = 1.0f // apply map resolution if needed
////            val centerX = robotPose.x * mapMetaData.resolution
////            val centerY = (mapMetaData.height - robotPose.y) * mapMetaData.resolution
////
////            // Move the robot to the center of the view if needed
////            matrix.postTranslate(-centerX, -centerY)
//
//            // Rotate the map around the robot's position
//            val degrees = Math.toDegrees(robotPose.theta).toFloat()
//            matrix.postRotate(degrees, rawBitmap.width / 2f, rawBitmap.height / 2f)
//
//            // Apply transformation to the bitmap
//            val transformedBitmap = Bitmap.createBitmap(
//                rawBitmap, 0, 0,
//                rawBitmap.width, rawBitmap.height,
//                matrix, true
//            )
//
//            Log.i("Ros2MapClient","theta: $degrees")
//            return transformedBitmap
        } catch (e: Exception) {
            Log.e("Ros2MapClient", "Error processing map data", e)
            mapMetaData=MapMetaData()
            return null
        }
    }

    //fun isValid(): Boolean = mapMetaData.validity
}