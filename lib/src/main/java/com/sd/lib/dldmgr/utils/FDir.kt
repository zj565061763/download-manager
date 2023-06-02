package com.sd.lib.dldmgr.utils

import com.sd.lib.dldmgr.utils.IDir.Companion.TempExt
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * [File]转[IDir]
 */
internal fun File.fDir(): IDir {
    if (this.isFile) error("this should not be a file")
    return DirImpl(this)
}

internal interface IDir {
    /**
     * 返回[key]对应的文件，如果key包括扩展名，则会使用[key]的扩展名
     */
    fun getKeyFile(key: String?): File?

    /**
     * 返回[key]对应的临时文件，扩展名：[TempExt]
     */
    fun getKeyTempFile(key: String?): File?

    /**
     * 删除当前文件夹下的文件（临时文件不会被删除）
     * @param ext 文件扩展名（例如mp3）null-删除所有文件
     * @param block 遍历文件，返回true则跳过该文件
     * @return 返回删除的文件数量
     */
    fun deleteFile(ext: String?, block: ((File) -> Boolean)? = null): Int

    /**
     * 删除临时文件
     * @param block 遍历临时文件，返回true则跳过该文件
     * @return 返回删除的文件数量
     */
    fun deleteTempFile(block: ((File) -> Boolean)? = null): Int

    /**
     * 操作当前文件夹的子级
     */
    fun <T> listFiles(block: (files: Array<File>?) -> T): T

    /**
     * 操作当前文件夹
     */
    fun <T> modify(block: (dir: File?) -> T): T

    companion object {
        const val TempExt = "temp"
    }
}

private class DirImpl(dir: File) : IDir {
    private val _dir = dir

    override fun getKeyFile(key: String?): File? {
        if (key.isNullOrEmpty()) return null
        return createKeyFile(
            key = key,
            ext = key.fExt(),
        )
    }

    override fun getKeyTempFile(key: String?): File? {
        if (key.isNullOrEmpty()) return null
        return createKeyFile(
            key = key,
            ext = TempExt,
        )
    }

    override fun deleteFile(ext: String?, block: ((File) -> Boolean)?): Int {
        return listFiles { files ->
            if (!files.isNullOrEmpty()) {
                val noneDotExt = if (ext.isNullOrEmpty()) ext else {
                    ext.fExtRemoveDot()
                }

                var count = 0
                for (item in files) {
                    val itemExt = item.extension
                    if (itemExt == TempExt) continue
                    if (noneDotExt == null || noneDotExt == itemExt) {
                        if (block != null && block(item)) continue
                        if (item.fDelete()) count++
                    }
                }
                count
            } else {
                0
            }
        }
    }

    override fun deleteTempFile(block: ((File) -> Boolean)?): Int {
        return listFiles { files ->
            if (!files.isNullOrEmpty()) {
                var count = 0
                for (item in files) {
                    if (block != null && block(item)) continue
                    if (item.extension == TempExt) {
                        if (item.fDelete()) count++
                    }
                }
                count
            } else {
                0
            }
        }
    }

    override fun <T> listFiles(block: (files: Array<File>?) -> T): T {
        return modify {
            val files = it?.listFiles()
            block(files)
        }
    }

    @Synchronized
    override fun <T> modify(block: (dir: File?) -> T): T {
        val directory = if (_dir.fMakeDirs()) _dir else null
        return block(directory)
    }

    private fun createKeyFile(
        key: String,
        ext: String,
    ): File? {
        if (key.isEmpty()) return null
        return modify { dir ->
            if (dir != null) {
                val filename = libMD5(key) + ext.fExtAddDot()
                dir.resolve(filename)
            } else {
                null
            }
        }
    }
}

/**
 * 检查文件夹是否存在，如果不存在则创建文件夹，如果已存在并且是文件则删除该文件并创建文件夹
 * @return 当前文件夹是否存在
 */
internal fun File?.fMakeDirs(): Boolean {
    try {
        if (this == null) return false
        if (!this.exists()) return this.mkdirs()
        if (this.isDirectory) return true
        if (this.isFile) this.delete()
        return this.mkdirs()
    } catch (e: Exception) {
        return e.libThrowOrReturn { false }
    }
}

/**
 * 删除文件或者目录
 */
internal fun File?.fDelete(): Boolean {
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

internal fun libMD5(value: String): String {
    val bytes = MessageDigest.getInstance("MD5").digest(value.toByteArray())
    return bytes.joinToString("") { "%02X".format(it) }
}

internal fun <T> Exception.libThrowOrReturn(block: () -> T): T {
    if (this is IOException) {
        this.printStackTrace()
        return block()
    } else {
        throw this
    }
}