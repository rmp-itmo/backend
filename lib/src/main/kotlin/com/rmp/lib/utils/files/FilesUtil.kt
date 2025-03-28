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

    fun upload(base64Encoded: String, fileName: String) {
        try {
            val bytes = Base64.getDecoder().decode(base64Encoded)
            val path = Path("${AppConf.fileLocation}/$fileName")
            path.writeBytes(bytes)
        } catch (e: Exception) {
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