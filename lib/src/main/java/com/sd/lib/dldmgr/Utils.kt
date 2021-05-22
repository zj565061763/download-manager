package com.sd.lib.dldmgr

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.webkit.MimeTypeMap
import com.sd.lib.dldmgr.directory.IDownloadDirectory
import java.io.File
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

    fun md5(value: String): String {
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
     * 拷贝文件
     */
    fun copyFile(fileFrom: File, fileTo: File): Boolean {
        return try {
            val fileTemp = File(fileTo.absolutePath + IDownloadDirectory.EXT_TEMP)
            fileFrom.copyTo(fileTemp, overwrite = true)
            return moveFile(fileTemp, fileTo)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 移动文件
     */
    fun moveFile(fileFrom: File, fileTo: File): Boolean {
        return try {
            delete(fileTo)
            fileFrom.renameTo(fileTo)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 删除[file]文件或者目录
     */
    fun delete(file: File?): Boolean {
        return try {
            file?.deleteRecursively() ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}