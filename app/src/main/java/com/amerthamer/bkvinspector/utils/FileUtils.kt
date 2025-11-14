package com.amerthamer.justicebringer.utils

import android.content.Context
import android.net.Uri
import android.widget.ArrayAdapter
import java.io.BufferedReader
import java.io.InputStreamReader

object FileUtils {

    fun readTextFile(context: Context, uri: Uri): List<String> {
        val list = mutableListOf<String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val clean = line?.trim()
                        if (!clean.isNullOrEmpty()) list.add(clean)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun listToAdapter(context: Context, list: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(context, android.R.layout.simple_spinner_item, list).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }
}
