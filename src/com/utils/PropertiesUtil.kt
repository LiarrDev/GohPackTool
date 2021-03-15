package com.utils

import java.io.File
import java.util.*

class PropertiesUtil(private val f: File) {
    fun setProperties(map: Map<String, String>) {
        val inputStream = f.inputStream()
        val properties = Properties()
        properties.load(inputStream)
        map.forEach { (key, value) ->
            properties.setProperty(key, value)
        }
        val outputStream = f.outputStream()
        properties.store(outputStream, "#--#")
        inputStream.close()
        outputStream.close()
    }
}