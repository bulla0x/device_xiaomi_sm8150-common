/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.utils

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

object FileUtils {
    @JvmStatic
    fun writeLine(fileName: String, value: String): Boolean {
        return try {
            val file = File(fileName)
            if (!file.exists()) {
                return false
            }
            FileOutputStream(file).use { writer ->
                writer.write(value.toByteArray())
            }
            true
        } catch (e: FileNotFoundException) {
            Logging.e("FileUtils", "File not found: $fileName")
            false
        } catch (e: IOException) {
            Logging.e("FileUtils", "Error writing to $fileName", e)
            false
        }
    }

    @JvmStatic
    fun fileExists(fileName: String): Boolean {
        return File(fileName).exists()
    }

    @JvmStatic
    fun isFileReadable(fileName: String): Boolean {
        val file = File(fileName)
        return file.exists() && file.canRead()
    }

    @JvmStatic
    fun isFileWritable(fileName: String): Boolean {
        val file = File(fileName)
        return file.exists() && file.canWrite()
    }

    @JvmStatic
    fun readOneLine(fileName: String): String? {
        val file = File(fileName)
        if (!file.exists()) return null
        return try {
            file.readText().trim()
        } catch (e: Exception) {
            Logging.e("FileUtils", "Error reading $fileName", e)
            null
        }
    }
}
