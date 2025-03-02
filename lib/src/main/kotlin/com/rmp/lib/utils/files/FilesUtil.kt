package com.rmp.lib.utils.files

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.exceptions.BadRequestException
import java.io.*
import java.util.*
import javax.imageio.*
import kotlin.io.path.*


object FilesUtil {
    fun buildName(file: String): String {
        val currentMillis = System.currentTimeMillis()

        val fileName = Path(file)
        return "${fileName.name}${currentMillis}.${fileName.extension}"
    }

    fun upload(base64Encoded: String, fileName: String, compressFileName : String) {
        try {
            val bytes = Base64.getDecoder().decode(base64Encoded)
            val path = Path("${AppConf.fileLocation}/$fileName")
            path.writeBytes(bytes)
        } catch (e: Exception) {
            throw BadRequestException("Bad file encoding")
        }
        try {
            val image = ImageIO.read(File("${AppConf.fileLocation}/$fileName"))
            val formatName = fileName.split(".").last()

            if (formatName == "webp") {
                val bytes = Base64.getDecoder().decode(base64Encoded)
                val path = Path("${AppConf.fileLocation}/$compressFileName")
                path.writeBytes(bytes)
                return
            }

            val writers = ImageIO.getImageWritersByFormatName(formatName)
            val writer = writers.next()

            val output = Path("${AppConf.fileLocation}/$compressFileName").toFile()

            val outputStream = ImageIO.createImageOutputStream(output)
            writer.output = outputStream
            val params = writer.defaultWriteParam
            params.compressionMode = ImageWriteParam.MODE_EXPLICIT
            params.compressionQuality = 0.5f

            writer.write(null, IIOImage(image, null, null), params)

            outputStream.close()
            writer.dispose()

        } catch (e : Exception){
            Path("${AppConf.fileLocation}/$fileName").deleteIfExists()
            throw BadRequestException("Bad file encoding")
        }
    }

    fun read(fileName: String): ByteArray? {
        return try {
            Path("${AppConf.fileLocation}/$fileName").readBytes()
        } catch (e: Exception) {
            null
        }
    }

    fun encodeBytes(bytes: ByteArray?): String {
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun removeFile(fileName: String): Boolean {

        return Path("${AppConf.fileLocation}/$fileName").deleteIfExists()
    }
}