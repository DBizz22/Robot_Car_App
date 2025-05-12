package com.example.controller.objects

import org.json.JSONArray
import org.json.JSONObject

open class Ros2Client() {
    fun advertise(topic: String, msgType: String){
        val advertiseMsg = JSONObject().apply {
            put("op", "advertise")
            put("topic", topic)
            put("type", msgType)
        }
        SharedWebSocketClient.getInstance().send(advertiseMsg.toString())
    }

    fun subscribe(topic: String, msgType: String) {
        val json = JSONObject()
        json.put("op", "subscribe")
        json.put("topic", topic)
        json.put("type", msgType)
        SharedWebSocketClient.getInstance().send(json.toString())
    }

    fun publish(topic: String, msgType: String, data: JSONObject) {
        val json = JSONObject()
        json.put("op", "publish")
        json.put("topic", topic)
        json.put("type", msgType)
        json.put("msg", data)
        SharedWebSocketClient.getInstance().send(json.toString())
    }

    fun requestService(name: String, args: JSONObject) {
        val json = JSONObject()
        json.apply {
            put("op", "call_service")
            put("service", name)
            put("args", args)
        }
        SharedWebSocketClient.getInstance().send(json.toString())
    }
//
//    fun requestAction(name: String, type: String, goal: JSONObject){
//        val actionMsg = JSONObject().apply {
//            put("op", "action_send_goal")
//            put("action_name", name)
//            put("type", type)
//            put("goal", goal)
//            put("goal_id", uuid)
//        }
//        SharedWebSocketClient.getInstance().send(actionMsg.toString())
//    }

    fun setParameter(nodeName: String, paramName: String, value: Any) {
        val paramValue = JSONObject()
        paramValue.put("type", 2) // Type 2 = Double (according to ROS2 parameter types)
        when(value)
        {
            is Int -> paramValue.put("integer_value",value)
            is Double -> paramValue.put("double_value", value)
        }

        val param = JSONObject()
        param.put("name", paramName)
        param.put("value", paramValue)

        val args = JSONObject()
        args.put("parameters", JSONArray().put(param))

        val message = JSONObject()
        message.put("op", "call_service")
        message.put("service", "/$nodeName/set_parameters")
        message.put("args", args)
        message.put("id", "set_param_${System.currentTimeMillis()}")

        SharedWebSocketClient.getInstance().send(message.toString())
    }
}