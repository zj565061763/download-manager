package com.sd.lib.dldmgr

import com.sd.lib.dldmgr.directory.IDownloadDirectory
import com.sd.lib.dldmgr.directory.IDownloadDirectory.FileInterceptor
import java.io.File

class DownloadDirectory : IDownloadDirectory {
    protected val directory: File

    private constructor(directory: File?) {
        this.directory = directory ?: File("path")
    }

    override fun checkExist(): Boolean {
        return Utils.checkDir(directory)
    }

    override fun getFile(url: String?): File? {
        val file = newUrlFile(url) ?: return null
        return if (file.exists()) file else null
    }

    override fun getFile(url: String?, defaultFile: File): File {
        return getFile(url) ?: defaultFile
    }

    override fun getTempFile(url: String?): File? {
        val file = newUrlTempFile(url) ?: return null
        return if (file.exists()) file else null
    }

    @Synchronized
    override fun copyFile(file: File): File {
        if (file.isDirectory) throw IllegalArgumentException("file must not be a directory")
        if (!file.exists()) return file

        val dir = directory
        if (!Utils.checkDir(dir)) return file

        val filename = file.name
        val tempFile = File(dir, filename + IDownloadDirectory.EXT_TEMP)
        if (!Utils.copyFile(file, tempFile)) {
            // 拷贝失败
            return file
        }

        val copyFile = File(dir, filename)
        Utils.delete(copyFile)

        return if (tempFile.renameTo(copyFile)) {
            copyFile
        } else {
            file
        }
    }

    @Synchronized
    override fun takeFile(file: File): File {
        if (file.isDirectory) throw IllegalArgumentException("file must not be a directory")
        if (!file.exists()) return file

        val dir = directory
        if (!Utils.checkDir(dir)) return file

        val newFile = File(dir, file.name)
        Utils.delete(newFile)

        return if (file.renameTo(newFile)) {
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
        if (files == null || files.isEmpty()) return 0

        var count = 0
        var delete = false
        for (file in files) {
            val name = file.name
            if (name.endsWith(IDownloadDirectory.EXT_TEMP)) continue

            if (ext == null) {
                delete = true
            } else {
                val itemExt = Utils.getExt(file.absolutePath)
                if (ext == itemExt) delete = true
            }

            if (delete && Utils.delete(file)) {
                count++
            }
        }
        return count
    }

    @Synchronized
    override fun deleteTempFile(interceptor: FileInterceptor?): Int {
        val files = getAllFile()
        if (files == null || files.isEmpty()) return 0

        var count = 0
        for (file in files) {
            if (interceptor != null && interceptor.intercept(file)) {
                continue
            }
            if (file.name.endsWith(IDownloadDirectory.EXT_TEMP)) {
                if (Utils.delete(file)) count++
            }
        }
        return count
    }

    private fun getAllFile(): Array<File>? {
        val dir = directory
        if (!Utils.checkDir(dir)) return null

        val files = dir.listFiles()
        return if (files == null || files.isEmpty()) null else files
    }

    fun newUrlFile(url: String?): File? {
        if (url == null || url.isEmpty()) {
            return null
        }
        val ext = Utils.getExt(url)
        return createUrlFile(url, ext)
    }

    fun newUrlTempFile(url: String?): File? {
        if (url == null || url.isEmpty()) {
            return null
        }
        val ext = IDownloadDirectory.EXT_TEMP
        return createUrlFile(url, ext)
    }

    private fun createUrlFile(url: String, ext: String?): File? {
        if (url.isEmpty()) return null

        val dir = directory
        if (!Utils.checkDir(dir)) return null

        val finalExt = if (ext == null || ext.isEmpty()) {
            ""
        } else {
            if (ext.startsWith(".")) ext else ".${ext}"
        }

        val fileName = Utils.md5(url) + finalExt
        return File(dir, fileName)
    }

    override fun hashCode(): Int {
        return directory.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is DownloadDirectory) return false
        return directory == other.directory
    }

    override fun toString(): String {
        return directory.toString()
    }

    companion object {
        @JvmStatic
        fun from(directory: File?): DownloadDirectory {
            return DownloadDirectory(directory)
        }
    }
}