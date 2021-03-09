package com.utils

import java.io.File
import java.util.*

class PropertiesUtil(private val f: File) {
    // FIXME: Stream 未关闭，修复后移到新方法上
    @Deprecated("Stream 未关闭")
    fun load(): Properties {
        val properties = Properties()
        properties.load(f.inputStream())
        return properties
    }

    fun setProperties(map: Map<String, String>) {
        val inputStream = f.inputStream()
        val outputStream = f.outputStream()
        val properties = Properties()
        properties.load(inputStream)
        map.forEach { (key, value) ->
            properties.setProperty(key, value)
        }
        properties.store(outputStream, "#--#")
        inputStream.close()
        outputStream.close()
    }
}