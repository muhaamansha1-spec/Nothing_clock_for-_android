package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.io.FileOutputStream

object CustomFontManager {
    private const val PREFS_NAME = "font_settings_prefs"
    private const val KEY_SELECTED_FONT = "selected_font_name"

    fun getFontDir(context: Context): File {
        val dir = File(context.filesDir, "custom_fonts")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getUploadedFontFiles(context: Context): List<File> {
        val dir = getFontDir(context)
        return dir.listFiles()?.filter { 
            it.isFile && (it.extension.equals("ttf", ignoreCase = true) || it.extension.equals("otf", ignoreCase = true))
        }?.sortedBy { it.name } ?: emptyList()
    }

    fun getSelectedFontName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_FONT, "NDOT57") ?: "NDOT57"
    }

    fun setSelectedFontName(context: Context, fontName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_FONT, fontName)
            .apply()
        
        // Notify all widgets to update text fonts immediately
        notifyWidgetsFontChanged(context)
    }

    fun notifyWidgetsFontChanged(context: Context) {
        try {
            com.example.ClockWidgetProvider.updateAllWidgets(context)
        } catch (_: Throwable) {}
        try {
            com.example.AlarmWidgetProvider.updateAllWidgets(context)
        } catch (_: Throwable) {}
        try {
            com.example.WeatherWidgetProvider.updateAllWidgets(context)
        } catch (_: Throwable) {}
        try {
            com.example.MusicWidgetProvider.updateAllWidgets(context)
        } catch (_: Throwable) {}
    }

    fun loadTypeface(context: Context, fontName: String? = null): Typeface {
        val activeName = fontName ?: getSelectedFontName(context)
        return try {
            when (activeName) {
                "NDOT57", "DEFAULT_NDOT" -> {
                    ResourcesCompat.getFont(context, com.example.R.font.dotmatrix) ?: Typeface.MONOSPACE
                }
                "DEFAULT_MONO" -> Typeface.MONOSPACE
                "DEFAULT_SANS" -> Typeface.SANS_SERIF
                "DEFAULT_SERIF" -> Typeface.SERIF
                else -> {
                    val file = File(getFontDir(context), activeName)
                    if (file.exists()) {
                        Typeface.createFromFile(file) ?: (ResourcesCompat.getFont(context, com.example.R.font.dotmatrix) ?: Typeface.MONOSPACE)
                    } else {
                        ResourcesCompat.getFont(context, com.example.R.font.dotmatrix) ?: Typeface.MONOSPACE
                    }
                }
            }
        } catch (e: Throwable) {
            Typeface.MONOSPACE
        }
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var fileName = "custom_font.ttf"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CustomFontManager", "Error querying filename from URI", e)
        }
        if (!fileName.endsWith(".ttf", ignoreCase = true) && !fileName.endsWith(".otf", ignoreCase = true)) {
            fileName += ".ttf"
        }
        return fileName
    }

    fun saveFontFromUri(context: Context, uri: Uri): Pair<Boolean, String> {
        return try {
            val rawName = getFileNameFromUri(context, uri)
            val cleanName = rawName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(getFontDir(context), cleanName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Pair(false, "Could not open file input stream")

            val typeface = Typeface.createFromFile(targetFile)
            if (typeface == null) {
                targetFile.delete()
                return Pair(false, "Invalid font file structure")
            }

            setSelectedFontName(context, cleanName)
            Pair(true, cleanName)
        } catch (e: Exception) {
            Log.e("CustomFontManager", "Error saving font from URI", e)
            Pair(false, e.localizedMessage ?: "Failed to save font file")
        }
    }

    fun deleteFont(context: Context, fontName: String): Boolean {
        return try {
            val file = File(getFontDir(context), fontName)
            val deleted = if (file.exists()) file.delete() else false
            if (getSelectedFontName(context) == fontName) {
                setSelectedFontName(context, "NDOT57")
            }
            deleted
        } catch (e: Exception) {
            false
        }
    }

    fun loadFontFamily(context: Context, fontName: String): FontFamily? {
        return try {
            when (fontName) {
                "NDOT57", "DEFAULT_NDOT" -> com.example.ui.theme.DotMatrixFontFamily
                "DEFAULT_MONO" -> FontFamily.Monospace
                "DEFAULT_SANS" -> FontFamily.SansSerif
                "DEFAULT_SERIF" -> FontFamily.Serif
                else -> {
                    val file = File(getFontDir(context), fontName)
                    if (file.exists()) {
                        try {
                            val typeface = Typeface.createFromFile(file)
                            if (typeface != null) {
                                FontFamily(typeface)
                            } else {
                                FontFamily.Monospace
                            }
                        } catch (e: Throwable) {
                            Log.e("CustomFontManager", "Error loading Typeface from ${file.absolutePath}", e)
                            FontFamily.Monospace
                        }
                    } else {
                        FontFamily.Monospace
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("CustomFontManager", "Error resolving font family for $fontName", e)
            FontFamily.Monospace
        }
    }
}
