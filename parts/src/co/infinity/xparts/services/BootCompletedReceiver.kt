/*
 * Copyright (C) 2015 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.infinity.xparts.popupcamera.PopupCameraUtils
import co.infinity.xparts.utils.Logging

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logging.i(TAG, "Received intent: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> handleBoot(context)
            Intent.ACTION_BOOT_COMPLETED -> handleBoot(context)
        }
    }

    private fun handleBoot(context: Context) {
        Logging.i(TAG, "Handling boot completed actions")
        PopupCameraUtils.checkPopupCameraService(context)

        // Invoke ThermalUtils if present
        try {
            val thermalClass = Class.forName("co.infinity.xparts.data.ThermalUtils")
            val getInstanceMethod = thermalClass.getMethod("getInstance", Context::class.java)
            val instance = getInstanceMethod.invoke(null, context)
            val startServiceMethod = thermalClass.getMethod("startService")
            startServiceMethod.invoke(instance)
        } catch (_: Exception) {}

        // Invoke PerfMonUtils if present
        try {
            val perfMonClass = Class.forName("co.infinity.xparts.data.PerfMonUtils")
            val getInstanceMethod = perfMonClass.getMethod("getInstance", Context::class.java)
            val instance = getInstanceMethod.invoke(null, context)
            val startServiceMethod = perfMonClass.getMethod("startServiceIfEnabled")
            startServiceMethod.invoke(instance)
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
