package com.example.controller.objects

import android.graphics.Bitmap
import com.example.controller.objects.Ros2PathClient
import com.example.controller.objects.Ros2PoseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object MapOverlayer {
    private var job: Job? = null
    var setImageListener: ((Bitmap) -> Unit)? = null

    fun process() {
        val mapMetaData = Ros2MapClient.mapMetaData
        val robotpose= Ros2PoseClient.getCurrentPose()
        if (!mapMetaData.validity)
            return
        val originalMap = Ros2MapClient.overlayMap(robotpose) ?: return
        //val bitmap = originalMap.copy(Bitmap.Config.ARGB_8888, true)
        //val bitmap = Ros2MapClient.overlayMap() ?: return
        val overlayedPoseBitmap= Ros2PoseClient.overlayPose(originalMap, mapMetaData) ?: return
        //val overlayedPathBitmap= Ros2PathClient.overlayPath(overlayedPoseBitmap,mapMetaData) ?: return
        val overlayedScanBitmap= Ros2LaserClient.overlayLaserScanData(overlayedPoseBitmap,mapMetaData,robotpose)?: return
        setImageListener?.invoke(overlayedScanBitmap)
    }

    fun startStream()
    {
        if (job?.isActive == true) return
        Ros2MapClient.startStream()
        Ros2PoseClient.startStream()
        //Ros2PathClient.startStream()
        Ros2LaserClient.startStream()
        Ros2Nav2Client.setup()
        job = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                process()
                delay(50)
            }
        }
    }

    fun stopStream() {
        job?.cancel()
        job = null
    }
}