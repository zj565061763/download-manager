package com.sd.lib.dldmgr.directory

import com.sd.lib.dldmgr.Utils
import com.sd.lib.dldmgr.Utils.fCopyToFile
import com.sd.lib.dldmgr.Utils.fCreateDir
import com.sd.lib.dldmgr.Utils.fDelete
import com.sd.lib.dldmgr.Utils.fMoveToFile
import com.sd.lib.dldmgr.directory.IDownloadDirectory.FileInterceptor
import com.sd.lib.dldmgr.utils.fGetExt
import com.sd.lib.dldmgr.utils.removePrefixDot
import java.io.File

/** 临时文件扩展名  */
private const val TempExt = "temp"

class DownloadDirectory private constructor(directory: File) : IDownloadDirectory {
    private val _directory = directory

    private fun createDir(): File? {
        return if (_directory.fCreateDir()) _directory else null
    }

    private fun getAllFile(): Array<File>? {
        val dir = createDir() ?: return null
        val files = dir.listFiles()
        return if (files.isNullOrEmpty()) null else files
    }

    override fun urlFile(url: String?, defaultFile: File?): File? {
        return modify {
            val file = newUrlFile(url)
            if (file?.exists() == true) file else defaultFile
        }
    }

    override fun urlTempFile(url: String?): File? {
        return modify {
            val file = newUrlTempFile(url)
            if (file?.exists() == true) file else null
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
                    removePrefixDot(ext)
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
    override fun deleteTempFile(interceptor: FileInterceptor?): Int {
        val files = getAllFile()
        if (files.isNullOrEmpty()) return 0

        var count = 0
        for (file in files) {
            if (interceptor != null && interceptor.intercept(file)) {
                continue
            }
            if (file.name.endsWith(TempExt)) {
                if (file.fDelete()) count++
            }
        }
        return count
    }

    @Synchronized
    fun <T> modify(block: (dir: File?) -> T): T {
        return block(createDir())
    }

    internal fun newUrlFile(url: String?): File? {
        if (url.isNullOrEmpty()) {
            return null
        }
        val ext = url.fGetExt()
        return createUrlFile(url, ext)
    }

    internal fun newUrlTempFile(url: String?): File? {
        if (url.isNullOrEmpty()) {
            return null
        }
        val ext = TempExt
        return createUrlFile(url, ext)
    }

    private fun createUrlFile(url: String, ext: String?): File? {
        if (url.isEmpty()) return null

        val dir = _directory
        if (!dir.fCreateDir()) return null

        val finalExt = if (ext.isNullOrEmpty()) {
            ""
        } else {
            if (ext.startsWith(".")) ext else ".${ext}"
        }

        val fileName = Utils.md5(url) + finalExt
        return File(dir, fileName)
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