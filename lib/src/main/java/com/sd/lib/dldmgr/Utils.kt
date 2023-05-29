package com.sd.lib.dldmgr

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.io.IOException
import java.security.MessageDigest

internal object Utils {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun postMainThread(runnable: Runnable) {
        mainHandler.post(runnable)
    }

    /**
     * 获取缓存目录下的[name]目录，如果name为空则获取缓存目录，
     * 缓存目录优先获取[Context.getExternalCacheDir]，如果不存在则获取[Context.getCacheDir]
     */
    @JvmOverloads
    fun fCacheDir(context: Context, name: String? = null): File {
        val dir = context.externalCacheDir ?: context.cacheDir ?: error("cache dir is unavailable")
        val ret = if (name.isNullOrEmpty()) {
            dir
        } else {
            dir.resolve(name)
        }
        return if (ret.fCreateDir()) ret else error("cache dir is unavailable")
    }

    /**
     * 拷贝文件
     */
    fun File?.fCopyToFile(file: File?): Boolean {
        try {
            if (this == null || file == null) return false
            if (!this.exists()) return false
            if (this.isDirectory) error("this should not be a directory")
            if (this == file) return true
            if (!file.fCreateFile()) return false
            this.copyTo(file, overwrite = true)
            return true
        } catch (e: Exception) {
            return e.libThrowOrReturn { false }
        }
    }

    /**
     * 移动文件
     */
    fun File?.fMoveToFile(file: File?): Boolean {
        try {
            if (this == null || file == null) return false
            if (!this.exists()) return false
            if (this.isDirectory) error("this should not be a directory")
            if (this == file) return true
            if (!file.fCreateFile()) return false
            return this.renameTo(file)
        } catch (e: Exception) {
            return e.libThrowOrReturn { false }
        }
    }

    /**
     * 检查文件是否存在，如果不存在则尝试创建，如果已存在则根据[overwrite]来决定是否覆盖，默认覆盖
     */
    @JvmOverloads
    fun File?.fCreateFile(overwrite: Boolean = true): Boolean {
        try {
            if (this == null) return false
            if (!this.exists()) return this.parentFile.fCreateDir() && this.createNewFile()
            if (overwrite) {
                this.fDelete()
            } else {
                if (this.isFile) return true
                this.deleteRecursively()
            }
            return this.parentFile.fCreateDir() && this.createNewFile()
        } catch (e: Exception) {
            return e.libThrowOrReturn { false }
        }
    }

    /**
     * 检查文件夹是否存在，如果不存在则尝试创建，如果已存在并且是文件则删除该文件并尝试创建文件夹
     * @return true-创建成功或者文件夹已经存在
     */
    fun File?.fCreateDir(): Boolean {
        try {
            if (this == null) return false
            if (!this.exists()) return this.mkdirs()
            if (this.isDirectory) return true
            this.fDelete()
            return this.mkdirs()
        } catch (e: Exception) {
            return e.libThrowOrReturn { false }
        }
    }

    /**
     * 删除文件或者目录
     */
    fun File?.fDelete(): Boolean {
        try {
            if (this == null) return false
            if (!this.exists()) return false
            return if (this.isFile) {
                this.delete()
            } else {
                this.deleteRecursively()
            }
        } catch (e: Exception) {
            return e.libThrowOrReturn { false }
        }
    }

    fun md5(value: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(value.toByteArray())
        return bytes.joinToString("") { "%02X".format(it) }
    }

    /**
     * 获取扩展名，不包括"."，
     * 例如：png
     */
    @JvmOverloads
    fun String?.fGetExt(defaultExt: String? = null): String {
        if (this.isNullOrEmpty()) {
            return formatDefaultExt(defaultExt)
        }

        var ext = MimeTypeMap.getFileExtensionFromUrl(this)
        if (ext.isNullOrEmpty()) {
            ext = this.substringAfterLast(delimiter = ".", missingDelimiterValue = "")
        }

        return if (ext.isEmpty()) {
            formatDefaultExt(defaultExt)
        } else {
            removePrefixDot(ext)
        }
    }

    private fun formatDefaultExt(defaultExt: String?): String {
        return if (defaultExt.isNullOrEmpty()) {
            ""
        } else {
            removePrefixDot(defaultExt)
        }
    }

    private fun removePrefixDot(input: String): String {
        var ret = input
        while (ret.startsWith(".")) {
            ret = ret.removePrefix(".")
        }
        return ret
    }
}

internal inline fun logMsg(block: () -> String) {
    if (DownloadManagerConfig.get().isDebug) {
        Log.i("FDownloadManager", block())
    }
}

internal fun libWhiteExceptionList(): List<Class<out Exception>> {
    return listOf(
        IOException::class.java,
        SecurityException::class.java,
    )
}

internal fun <T> Exception.libThrowOrReturn(
    whiteList: List<Class<out Exception>> = libWhiteExceptionList(),
    block: () -> T,
): T {
    if (this.javaClass in whiteList) {
        this.printStackTrace()
        return block()
    } else {
        throw this
    }
}