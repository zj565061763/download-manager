package com.sd.lib.dldmgr.directory

import com.sd.lib.dldmgr.utils.Utils
import com.sd.lib.dldmgr.utils.Utils.fCopyToFile
import com.sd.lib.dldmgr.utils.Utils.fCreateDir
import com.sd.lib.dldmgr.utils.Utils.fDelete
import com.sd.lib.dldmgr.utils.Utils.fMoveToFile
import com.sd.lib.dldmgr.utils.fDotExt
import com.sd.lib.dldmgr.utils.fGetExt
import com.sd.lib.dldmgr.utils.fNoneDotExt
import java.io.File

/** 临时文件扩展名  */
private const val TempExt = "temp"

class DownloadDirectory private constructor(directory: File) : IDownloadDirectory {
    private val _directory = directory

    private fun createDir(): File? {
        return if (_directory.fCreateDir()) _directory else null
    }

    override fun urlFile(url: String?, defaultFile: File?): File? {
        if (url.isNullOrEmpty()) return null
        return modify {
            val file = newUrlFile(url)
            if (file?.exists() == true) file else defaultFile
        }
    }

    override fun copyFile(file: File): File {
        return modify { dir ->
            if (dir != null && file.exists()) {
                if (file.isDirectory) error("file should not be a directory")
                val newFile = dir.resolve(file.name)
                if (file.fCopyToFile(newFile)) newFile else file
            } else {
                file
            }
        }
    }

    override fun takeFile(file: File): File {
        return modify { dir ->
            if (dir != null && file.exists()) {
                if (file.isDirectory) error("file should not be a directory")
                val newFile = dir.resolve(file.name)
                if (file.fMoveToFile(newFile)) newFile else file
            } else {
                file
            }
        }
    }

    override fun deleteFile(ext: String?): Int {
        return modify { dir ->
            val files = dir?.listFiles()
            if (!files.isNullOrEmpty()) {
                val formatDot = if (ext.isNullOrEmpty()) ext else {
                    ext.fNoneDotExt()
                }

                var count = 0
                for (item in files) {
                    val itemExt = item.extension
                    if (itemExt == TempExt) continue

                    if (formatDot == null) {
                        if (item.fDelete()) count++
                    } else {
                        if (formatDot == itemExt) {
                            if (item.fDelete()) count++
                        }
                    }
                }
                count
            } else {
                0
            }
        }
    }

    @Synchronized
    override fun <T> modify(block: (dir: File?) -> T): T {
        return block(createDir())
    }

    internal fun deleteTempFile(interceptor: (File) -> Boolean): Int {
        return modify { dir ->
            val files = dir?.listFiles()
            if (!files.isNullOrEmpty()) {
                var count = 0
                for (item in files) {
                    if (interceptor(item)) continue
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

    internal fun newUrlFile(url: String): File? {
        if (url.isEmpty()) return null
        val ext = url.fGetExt()
        return createUrlFile(url, ext)
    }

    internal fun newUrlTempFile(url: String): File? {
        if (url.isEmpty()) return null
        val ext = TempExt
        return createUrlFile(url, ext)
    }

    private fun createUrlFile(url: String, ext: String): File? {
        if (url.isEmpty()) return null
        return modify { dir ->
            if (dir != null) {
                val filename = Utils.md5(url) + ext.fDotExt()
                dir.resolve(filename)
            } else {
                null
            }
        }
    }

    override fun hashCode(): Int {
        return _directory.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is DownloadDirectory) return false
        return _directory == other._directory
    }

    override fun toString(): String {
        return _directory.toString()
    }

    companion object {
        @JvmStatic
        fun from(directory: File): DownloadDirectory {
            return DownloadDirectory(directory)
        }
    }
}