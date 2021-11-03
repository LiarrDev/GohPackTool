package goh.utils

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object CommandUtil {
    fun exec(command: String): Boolean {
        println("【Command】 $command")
        val process: Process = Runtime.getRuntime().exec(command)
        val errorStream = StreamGobbler(process.errorStream, "ERROR")
        val outputStream = StreamGobbler(process.inputStream, "OUTPUT")
        errorStream.start()
        outputStream.start()

        return if (process.waitFor() == 0) {
            println("【Command execute succeed】  $command")
            true
        } else {
            println("【Command execute failed】   $command")
            false
        }
    }

    fun decompile(apk: String, decompileDir: String, apktool: String): Boolean {
        val apkFile = File(apk)
        return if (apkFile.exists() && apkFile.isFile) {
            val decompileFile = File(decompileDir)
            if (decompileFile.exists()) {
                FileUtil.delete(decompileFile)
            }
            val command = "java -jar $apktool d -f $apk -o $decompileDir --only-main-classes"
            exec(command)
        } else {
            print("APK is not exist.")
            false
        }
    }
}

fun String.command(): Boolean {
    val process = this.execute()
    StreamGobbler(process.errorStream, "ERROR").start()
    StreamGobbler(process.inputStream, "OUTPUT").start()

    return if (process.waitFor() == 0) {
        println("【Command execute succeed】  $this")
        true
    } else {
        println("【Command execute failed】   $this")
        false
    }
}

fun String.execute(): Process {
    return Runtime.getRuntime().exec(this)
}

fun Process.text(): String {
    val inputStream = this.inputStream
    val isr = InputStreamReader(inputStream)
    val reader = BufferedReader(isr)
    var line: String? = ""
    var output = ""
    while (line != null) {
        line = reader.readLine()
        output += line + "\n"
    }
    return output
}