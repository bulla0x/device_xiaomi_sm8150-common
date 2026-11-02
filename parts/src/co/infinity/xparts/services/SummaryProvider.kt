/*
 * Copyright (C) 2019 The Android Open Source Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.services

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import co.infinity.xparts.R

/** Provide preference summary for injected items. */
class SummaryProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val context = context ?: return null
        val summary = when (method) {
            METHOD_THERMAL -> {
                try {
                    val thermalClass = Class.forName("co.infinity.xparts.data.ThermalUtils")
                    val getInstanceMethod = thermalClass.getMethod("getInstance", android.content.Context::class.java)
                    val instance = getInstanceMethod.invoke(null, context)
                    val enabledField = thermalClass.getMethod("getEnabled").invoke(instance) as Boolean
                    context.getString(if (enabledField) R.string.tile_on else R.string.tile_off)
                } catch (e: Exception) {
                    null
                }
            }
            METHOD_PERFMON -> {
                try {
                    val perfMonClass = Class.forName("co.infinity.xparts.data.PerfMonUtils")
                    val getInstanceMethod = perfMonClass.getMethod("getInstance", android.content.Context::class.java)
                    val instance = getInstanceMethod.invoke(null, context)
                    val enabledField = perfMonClass.getMethod("isEnabled").invoke(instance) as Boolean
                    context.getString(if (enabledField) R.string.tile_on else R.string.tile_off)
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
        return if (summary != null) {
            Bundle().apply { putString(KEY_SUMMARY, summary) }
        } else {
            null
        }
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0

    companion object {
        private const val KEY_SUMMARY = "com.android.settings.summary"
        private const val METHOD_THERMAL = "thermalSummary"
        private const val METHOD_PERFMON = "perfmonSummary"
    }
}
