package goh.utils

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object DrawableUtil {

    fun replaceIcon(decompileDir: String, icon: File, sizeTag: String, iconName: String) {
        val mipmap =
            decompileDir + File.separator + "res" + File.separator + "mipmap-" + sizeTag + File.separator + iconName
        icon.replace(File(mipmap))

        val drawable =
            decompileDir + File.separator + "res" + File.separator + "drawable-" + sizeTag + File.separator + iconName
        icon.replace(File(drawable))

        val drawableV4 = decompileDir + File.separator + "res" + File.separator + "drawable-" + sizeTag + "-v4"
        if (File(drawableV4).exists()) {
            icon.replace(File(drawableV4 + File.separator + iconName))
        }
    }

    fun resizeImage(imgPath: String, width: Int, height: Int, newImgPath: String): File? {
        if (imgPath.isEmpty()) {
            return null
        }
        try {
            val src = ImageIO.read(File(imgPath))
            var imgColorMode = BufferedImage.TYPE_INT_RGB
            val suffix = imgPath.substring(imgPath.lastIndexOf(".") + 1)
            if ("png" == suffix) {
                imgColorMode = BufferedImage.TYPE_INT_ARGB
            }
            val image = BufferedImage(width, height, imgColorMode)
            image.graphics.drawImage(src, 0, 0, width, height, null)
            val file = File("$newImgPath\\app_icon.png")
            if (!file.exists()) {
                file.mkdirs()
            }
            ImageIO.write(image, suffix, file)
            return file
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 替换主体 LOGO 资源
     */
    fun replaceCoDrawable(decompileDir: String, coDrawableDir: String) {
        File(coDrawableDir).getDirectoryList().forEach {
            when (it.name) {
                "drawable-hdpi", "drawable-xhdpi", "drawable-xxhdpi" -> {
                    val destDir = File(decompileDir + File.separator + "res" + File.separator + it.name)
                    it.copyDirTo(destDir)
                    println("--> ${it.name} 素材替换完成")
                }
            }
        }
    }
}