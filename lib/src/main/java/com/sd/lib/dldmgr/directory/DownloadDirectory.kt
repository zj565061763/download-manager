package com.sd.lib.dldmgr.directory

import com.sd.lib.dldmgr.Utils
import com.sd.lib.dldmgr.Utils.fCopyToFile
import com.sd.lib.dldmgr.Utils.fCreateDir
import com.sd.lib.dldmgr.Utils.fDelete
import com.sd.lib.dldmgr.Utils.fGetExt
import com.sd.lib.dldmgr.Utils.fMoveToFile
import com.sd.lib.dldmgr.directory.IDownloadDirectory.FileInterceptor
import java.io.File

class DownloadDirectory private constructor(directory: File) : IDownloadDirectory {
    private val _directory = directory

    private fun createDir(): File? {
        return if (_directory.fCreateDir()) _directory else null
    }

    override fun urlFile(url: String?, defaultFile: File?): File? {
        val file = newUrlFile(url)
        return if (file?.exists() == true) file else defaultFile
    }

    override fun urlTempFile(url: String?): File? {
        val file = newUrlTempFile(url) ?: return null
        return if (file.exists()) file else null
    }

    @Synchronized
    override fun copyFile(file: File): File {
        if (!file.exists()) return file
        if (file.isDirectory) error("file should not be a directory")
        val dir = createDir() ?: return file
        val newFile = dir.resolve(file.name)
        return if (file.fCopyToFile(newFile)) newFile else file
    }

    @Synchronized
    override fun takeFile(file: File): File {
        if (!file.exists()) return file
        if (file.isDirectory) throw IllegalArgumentException("file must not be a directory")

        val dir = _directory
        if (!dir.fCreateDir()) return file

        val newFile = File(dir, file.name)
        return if (file.fMoveToFile(newFile)) {
            newFile
        } else {
            file
        }
    }

    @Synchronized
    override fun deleteFile(ext: String?): Int {
        if (ext != null && ext.startsWith(".")) {
            throw IllegalArgumentException("ext should not start with dot ${ext}")
        }

        val files = getAllFile()
        if (files.isNullOrEmpty()) return 0

        var count = 0
        var delete = false
        for (file in files) {
            val name = file.name
            if (name.endsWith(IDownloadDirectory.EXT_TEMP)) continue

            if (ext == null) {
                delete = true
            } else {
                val itemExt = file.absolutePath.fGetExt()
                if (ext == itemExt) delete = true
            }

            if (delete && file.fDelete()) {
                count++
            }
        }
        return count
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
            if (file.name.endsWith(IDownloadDirectory.EXT_TEMP)) {
                if (file.fDelete()) count++
            }
        }
        return count
    }

    private fun getAllFile(): Array<File>? {
        val dir = _directory
        if (!dir.fCreateDir()) return null

        val files = dir.listFiles()
        return if (files == null || files.isEmpty()) null else files
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
        val ext = IDownloadDirectory.EXT_TEMP
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