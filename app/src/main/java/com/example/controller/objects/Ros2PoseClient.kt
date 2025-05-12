package com.example.controller.objects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.Log
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object Ros2PoseClient : Ros2Client() {
    data class Pose2D(val x: Double = 0.0, val y: Double = 0.0, val theta: Double = 0.0)

    // Latest robot pose
    private var currentPose: Pose2D = Pose2D()

    // Robot car
    var robotBitmap: Bitmap? = null

    // Listener callback for pose updates
    private var poseListener: ((Pose2D) -> Unit)? = null

    fun eulerToQuaternion(
        roll: Double,
        pitch: Double,
        yaw: Double
    ): Quadruple<Double, Double, Double, Double> {
        val cy = cos(yaw * 0.5)
        val sy = sin(yaw * 0.5)
        val cr = cos(roll * 0.5)
        val sr = sin(roll * 0.5)
        val cp = cos(pitch * 0.5)
        val sp = sin(pitch * 0.5)

        val w = cy * cr * cp + sy * sr * sp
        val x = cy * sr * cp - sy * cr * sp
        val y = cy * cr * sp + sy * sr * cp
        val z = sy * cr * cp - cy * sr * sp

        return Quadruple(x, y, z, w)
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    fun publishInitialPose(x: Double, y: Double, thetaRad: Double) {
        val message = JSONObject().apply {
            put("op", "publish")
            put("topic", "/initialpose")
            put("msg", JSONObject().apply {
                put("header", JSONObject().apply {
                    put("frame_id", "map")
                    put("stamp", JSONObject().apply {
                        put("sec", System.currentTimeMillis() / 1000)
                        put("nanosec", (System.currentTimeMillis() % 1000) * 1_000_000)
                    })
                })
                put("pose", JSONObject().apply {
                    put("pose", JSONObject().apply {
                        put("position", JSONObject().apply {
                            put("x", x)
                            put("y", y)
                            put("z", 0.0)
                        })
                        put("orientation", JSONObject().apply {
                            //val q = eulerToQuaternion(0.0, 0.0, thetaRad)
//                            put("x", q.first)
//                            put("y", q.second)
//                            put("z", q.third)
//                            put("w", q.fourth)
                            put("x", 0)
                            put("y", 0)
                            put("z", 0)
                            put("w", 1)
                        })
                    })
//                    put(
//                        "covariance", JSONArray(
//                            listOf(
//                                // Default low uncertainty on position (x, y), more on orientation
//                                0.01, 0.0,  0.0, 0.0, 0.0, 0.0,   // X (high certainty)
//                                0.0,  0.01, 0.0, 0.0, 0.0, 0.0,   // Y (high certainty)
//                                0.0,  0.0,  0.0, 0.0, 0.0, 0.0,   // Z (ignored in 2D)
//                                0.0,  0.0,  0.0, 0.0, 0.0, 0.0,   // Unused (3D rotation)
//                                0.0,  0.0,  0.0, 0.0, 0.0, 0.0,   // Unused
//                                0.0,  0.0,  0.0, 0.0, 0.0, 0.0017 // Yaw (high certainty, ~2.5° std dev)
//                            )
//                        )
//                    )
                })
            })
        }

        SharedWebSocketClient.getInstance().send(message.toString())
    }


    fun subscribeToPoseTopic() {
        try {
            //publishInitialPose(0.0, 0.0, 0.0)
            subscribe("/robot_pose", "geometry_msgs/msg/PoseStamped")
        } catch (e: Exception) {
            Log.e("Ros2PoseClient", "Failed to parse pose message", e)
        }
    }

    fun setPoseUpdateListener(listener: (Pose2D) -> Unit) {
        poseListener = listener
    }

    fun startStream() {
        SharedWebSocketClient.poseHandler = {
            parsePose2D(it)
        }
        subscribeToPoseTopic()
    }

    private fun parsePose2D(msg: JSONObject?) {
        msg?.let {
            try {
                val header = msg.getJSONObject("header")
                val frameId = header.getString("frame_id")
                val stamp = header.getJSONObject("stamp")
                val timestampSec = stamp.getLong("sec") + stamp.getLong("nanosec") / 1e9

                val poseJson = msg.getJSONObject("pose")
                val position = poseJson.getJSONObject("position")
                val orientation = poseJson.getJSONObject("orientation")

                val x = position.optDouble("x", 0.0)
                val y = position.optDouble("y", 0.0)

                val ox = orientation.optDouble("x", 0.0)
                val oy = orientation.optDouble("y", 0.0)
                val oz = orientation.optDouble("z", 0.0)
                val ow = orientation.optDouble("w", 1.0)

                // Convert quaternion to yaw
                val siny_cosp = 2.0 * (ow * oz + ox * oy)
                val cosy_cosp = 1.0 - 2.0 * (oy * oy + oz * oz)
                val theta = atan2(siny_cosp, cosy_cosp)

                currentPose = Pose2D(x, y, theta)
                Log.e("Ros2PoseClient", "Current Pose: $x $y $theta")
            } catch (e: Exception) {
                Log.e("Ros2PoseClient", "Error parsing pose", e)
                null
            }
        }
    }

    fun overlayPose(mapBitmap: Bitmap, mapInfo: Ros2MapClient.MapMetaData): Bitmap? {
        try {
            val overlayBitmap = mapBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(overlayBitmap)

            val resolution = mapInfo.resolution
            val originX = mapInfo.originX
            val originY = mapInfo.originY

            val width = mapBitmap.width
            val height = mapBitmap.height

            // Robot position in pixels
            val robotPose = currentPose
            //Log.e("Ros2PoseClient", "Robot Pose: ${robotPose.x} ${robotPose.y} ${robotPose.theta}")
            val px = ((robotPose.x - originX) / resolution).toFloat()
            val py = (height - 1) - ((robotPose.y - originY) / resolution).toFloat()


            // Rotate robot bitmap to match theta
            robotBitmap?.let {
                val robotWidthMeters = 0.5
                val robotHeightMeters = 0.5
                val robotWidthPx = (robotWidthMeters / resolution).toInt()
                val robotHeightPx = (robotHeightMeters / resolution).toInt()

                val scaledBitmap = Bitmap.createScaledBitmap(it, robotWidthPx, robotHeightPx, true)

                val matrix = Matrix().apply {
                    postTranslate(-scaledBitmap.width / 2f, -scaledBitmap.height / 2f)
                    postRotate(Math.toDegrees(robotPose.theta).toFloat())
                    postTranslate(px, py)
                }
//                val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//                    isFilterBitmap = true
//                }

                canvas.drawBitmap(scaledBitmap, matrix, null)
            }
            return overlayBitmap


//            val width = mapBitmap.width
//            val height = mapBitmap.height
//
//            val robotPose = currentPose
//
//            val px = ((robotPose.x - mapInfo.originX) / mapInfo.resolution).toFloat()
//            val py = (height - 1) - ((robotPose.y - mapInfo.originY) / mapInfo.resolution).toFloat()
//
//            val centerX = width / 2f
//            val centerY = height / 2f
//
//            val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//            val canvas = Canvas(output)
//
//            // Move the map so the robot appears at canvas center
//            val mapMatrix = Matrix().apply {
//                postTranslate(centerX - px, centerY - py)
//            }
//            canvas.drawBitmap(mapBitmap, mapMatrix, null)
//
//            // Draw robot at canvas center with rotation
//            robotBitmap?.let {
//                val robotWidthPx = (0.5 / mapInfo.resolution).toInt()
//                val robotHeightPx = (0.5 / mapInfo.resolution).toInt()
//                val scaledBitmap = Bitmap.createScaledBitmap(it, robotWidthPx, robotHeightPx, true)
//
//                val robotMatrix = Matrix().apply {
//                    postTranslate(
//                        -scaledBitmap.width / 2f,
//                        -scaledBitmap.height / 2f
//                    ) // center bitmap
//                    postRotate(Math.toDegrees(robotPose.theta).toFloat())              // rotate
//                    postTranslate(
//                        centerX,
//                        centerY
//                    )                                     // move to center
//                }
//
//                canvas.drawBitmap(scaledBitmap, robotMatrix, null)
//            }
//            return output
        } catch (e: Exception) {
            Log.e("Ros2PoseClient", "Error processing pose data", e)
            return null
        }
    }

    fun getCurrentPose() = currentPose
}