package com.utils

import java.io.File
import java.util.*

class PropertiesUtil(private val f: File) {
    fun load(): Properties {
        val properties = Properties()
        properties.load(f.inputStream())
        return properties
    }
}