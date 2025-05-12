package com.example.controller.objects

import org.json.JSONObject
import org.json.JSONArray
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


data class NavigationGoal(val x: Double, val y: Double, val degree: Double)

class NavigationGoalManager {
    private val goalQueue = ArrayDeque<NavigationGoal>()
    private var currentGoalUUID: ByteArray? = null
    var currentGoalIndex = 0

    fun generateUUID(): ByteArray {
        return ByteArray(16) { Random.nextInt(0, 256).toByte() }.also {
            currentGoalUUID = it
        }
    }

    fun getCurrentUUIDArray(): JSONArray? {
        return currentGoalUUID?.let {
            JSONArray().apply { it.forEach { byte -> put(byte.toUByte().toInt()) } }
        }
    }

    fun getCurrentUUIDString(): String {
        return currentGoalUUID?.joinToString("") { "%02x".format(it) } ?: ""
    }

    fun addGoal(goal: NavigationGoal) = goalQueue.addLast(goal)
    fun removeGoal() = goalQueue.removeLastOrNull()
    fun nextGoal(): NavigationGoal? {
        currentGoalIndex++
        return goalQueue.removeFirstOrNull()
    }
    fun clearGoals() {
        currentGoalIndex=0
        goalQueue.clear()
    }
    fun hasGoals() = goalQueue.isNotEmpty()
    fun totalGoals() = goalQueue.size
}

object Ros2Nav2Client : Ros2Client() {
    private val goalManager = NavigationGoalManager()
    private var job: Job? = null
    private var isNavigating = false
    private var currentStatus = "Idle"

    var onStatusChange: ((String) -> Unit)? = null
    var onCompletionHandler: (() -> Unit)? = null
    var onErrorHandler: ((String) -> Unit)? = null

    fun setup(){
        try {
            subscribe("/navigate_to_pose/_action/status","action_msgs/msg/GoalStatusArray")
        } catch (e: Exception) {
            Log.e("Ros2Nav2Client", "Failed to subscribe to status topic", e)
        }

        SharedWebSocketClient.nav2StatusHandler={
            handleFeedback(it)
        }

        //advertiseNavigateToPoseGoal()
    }

    private fun reset()
    {
        goalManager.clearGoals()
        goalManager.currentGoalIndex=0
        updateStatus("Idle")
    }

    fun addGoal(x: Double, y: Double, degree: Double) {
        if(!goalManager.hasGoals())
            reset()
        goalManager.addGoal(NavigationGoal(x, y, degree))
    }

    fun removeGoal() {
        goalManager.removeGoal()
    }

    fun startNavigation() {
        if(!goalManager.hasGoals())
        {
            onErrorHandler?.invoke("No Goal")
            return
        }
        if (job?.isActive == true) return
        job = CoroutineScope(Dispatchers.IO).launch {
            while (goalManager.hasGoals()) {
                if (!isNavigating) {
                    goalManager.nextGoal()?.let { sendGoal(it) }
                }
                delay(2000)
            }
        }
    }

    private fun sendGoal(goal: NavigationGoal) {
        isNavigating = true
        val uuid = goalManager.generateUUID()
        val uuidArray = goalManager.getCurrentUUIDArray() ?: JSONArray()

        val theta = Math.toRadians(goal.degree)
        val msg = JSONObject().apply {
            put("header", JSONObject().apply {
                put("frame_id", "map")
                put("stamp", JSONObject().apply {
                    put("sec", System.currentTimeMillis() / 1000)
                    put("nanosec", (System.nanoTime() % 1_000_000_000))
                })
            })
            put("pose", JSONObject().apply {
                put("position", JSONObject().apply {
                    put("x", goal.x)
                    put("y", goal.y)
                    put("z", 0.0)
                })
                put("orientation", JSONObject().apply {
                    put("x", 0.0)
                    put("y", 0.0)
                    put("z", sin(theta/2))
                    put("w", cos(theta/2))
                })
            })
        }

        try {
            publish("/goal_pose", "geometry_msgs/msg/PoseStamped", msg)
            Log.d("Ros2Nav2Client", "Goal published to Nav2")
        } catch (e: Exception) {
            Log.e("Ros2Nav2Client", "Failed to publish goal", e)
            //onErrorHandler?.invoke("Failed to publish goal")
        }
    }

    fun cancelNavigation() {
        val cancelAllMsg = JSONObject().apply {
            put("goal_info", JSONObject().apply {
                put("goal_id", JSONObject().apply {
                    // UUID of all zeros = request to cancel all goals
                    put("uuid", JSONArray(List(16) { 0 }))
                })
            })
        }
        try {
            requestService("/navigate_to_pose/_action/cancel", cancelAllMsg)
            stop()
            updateStatus("Cancelled")
        } catch (e: Exception) {
            //onErrorHandler?.invoke("Cancel Failed")
        }
    }

    private fun stop() {
        goalManager.clearGoals()
        job?.cancel()
        job = null
        isNavigating = false
        onCompletionHandler?.invoke()
    }

    private fun updateStatus(status: String) {
        currentStatus = status
        onStatusChange?.invoke(status)
    }

    private fun handleFeedback(msg: JSONObject?) {
        val statusList = msg?.optJSONArray("status_list") ?: return
        for (i in 0 until statusList.length()) {
            val statusObj = statusList.getJSONObject(i)
            val status = statusObj.optInt("status")

            when (status) {
                1 -> updateStatus("Accepted")
                2 -> updateStatus("Executing")
                3 -> updateStatus("Cancelling")
                4 -> {
                    isNavigating = false
                    updateStatus("Successful")
                    if(!goalManager.hasGoals())
                        onCompletionHandler?.invoke()
                }
                5 -> {
                    stop()
                    updateStatus("Rejected")
                }
                6 -> {
                    stop()
                    updateStatus("Aborted")
                }

                else -> updateStatus("Unknown Status: $status")
            }
        }
    }

    fun getStatus(): String = currentStatus
    fun isRunning(): Boolean = isNavigating
    fun getGoalCount(): Int = goalManager.currentGoalIndex
    fun totalCount(): Int = goalManager.totalGoals() + goalManager.currentGoalIndex
}


