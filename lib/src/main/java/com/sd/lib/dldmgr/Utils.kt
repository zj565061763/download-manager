package com.sd.lib.dldmgr

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.webkit.MimeTypeMap
import java.io.*
import java.nio.channels.FileChannel
import java.security.MessageDigest

internal object Utils {
    private val HANDLER = Handler(Looper.getMainLooper())

    @JvmStatic
    fun postMainThread(runnable: Runnable) {
        HANDLER.post(runnable)
    }

    fun getCacheDir(name: String, context: Context): File {
        return if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            File(context.externalCacheDir, name)
        } else {
            File(context.cacheDir, name)
        }
    }

    fun checkDir(dir: File?): Boolean {
        if (dir == null) return false
        if (dir.exists()) return true
        return try {
            dir.mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun md5(value: String): String? {
        val bytes = MessageDigest.getInstance("MD5").apply {
            this.update(value.toByteArray())
        }.digest()

        val builder = StringBuilder()
        for (i in bytes.indices) {
            val hex = Integer.toHexString(0xFF and bytes[i].toInt())
            if (hex.length == 1) {
                builder.append('0')
            }
            builder.append(hex)
        }
        return builder.toString()
    }

    fun getExt(url: String?): String {
        if (url == null) return ""

        var ext = MimeTypeMap.getFileExtensionFromUrl(url)
        if (ext == null || ext.isEmpty()) {
            val lastIndex = url.lastIndexOf(".")
            if (lastIndex > 0) {
                ext = url.substring(lastIndex + 1)
            }
        }
        return ext ?: ""
    }

    /**
     * 删除文件或者目录
     *
     * @param file
     * @return
     */
    fun delete(file: File?): Boolean {
        if (file == null || !file.exists()) return true
        if (file.isFile) return file.delete()
        val files = file.listFiles()
        if (files != null) {
            for (item in files) {
                delete(item)
            }
        }
        return file.delete()
    }

    /**
     * 拷贝文件
     *
     * @param fileFrom
     * @param fileTo
     * @return
     */
    fun copyFile(fileFrom: File?, fileTo: File?): Boolean {
        if (!checkFile(fileFrom, fileTo)) return false
        var inputStream: FileInputStream? = null
        var outputStream: FileOutputStream? = null
        var inputChannel: FileChannel? = null
        var outputChannel: FileChannel? = null
        return try {
            inputStream = FileInputStream(fileFrom)
            outputStream = FileOutputStream(fileTo)
            inputChannel = inputStream.channel
            outputChannel = outputStream.channel
            inputChannel.transferTo(0, inputChannel.size(), outputChannel)
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        } finally {
            closeQuietly(inputStream)
            closeQuietly(outputStream)
            closeQuietly(inputChannel)
            closeQuietly(outputChannel)
        }
    }

    /**
     * 检查源文件和目标文件
     *
     * @param fileFrom
     * @param fileTo
     * @return
     */
    private fun checkFile(fileFrom: File?, fileTo: File?): Boolean {
        if (fileFrom == null || !fileFrom.exists()) return false
        require(!fileFrom.isDirectory) { "fileFrom must not be a directory" }
        if (fileTo == null) return false
        if (fileTo.exists()) {
            require(!fileTo.isDirectory) { "fileTo must not be a directory" }
            if (!fileTo.delete()) return false
        }
        val fileToParent = fileTo.parentFile
        if (fileToParent != null && !fileToParent.exists()) {
            if (!fileToParent.mkdirs()) return false
        }
        return true
    }

    fun closeQuietly(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (ignored: Throwable) {
            }
        }
    }
}