package com.scooter.control.data

import android.content.Context
import android.hardware.camera2.CameraManager

/** Включает/выключает настоящий фонарик телефона (используется кнопкой "headlight"). */
object Flashlight {
    fun set(context: Context, on: Boolean) {
        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = manager.cameraIdList.firstOrNull { cid ->
                manager.getCameraCharacteristics(cid)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            manager.setTorchMode(id, on)
        } catch (_: Exception) {
            // Нет фонарика / нет доступа к камере — просто игнорируем, это не критично.
        }
    }
}
