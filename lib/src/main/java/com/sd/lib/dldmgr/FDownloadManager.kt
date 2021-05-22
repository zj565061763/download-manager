package com.sd.lib.dldmgr

import android.util.Log
import com.sd.lib.dldmgr.directory.IDownloadDirectory.FileInterceptor
import com.sd.lib.dldmgr.exception.DownloadHttpException
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class FDownloadManager : IDownloadManager {
    private val _downloadDirectory: DownloadDirectory

    private val _mapDownloadInfo: MutableMap<String, DownloadInfoWrapper> = ConcurrentHashMap()
    private val _mapTempFile: MutableMap<File, String> = ConcurrentHashMap()

    private val _callbackHolder: MutableMap<IDownloadManager.Callback, String> = ConcurrentHashMap()

    protected constructor(directory: String) {
        if (directory.isEmpty()) throw IllegalArgumentException("directory is empty")
        _downloadDirectory = DownloadDirectory.from(File(directory))
    }

    @Synchronized
    override fun addCallback(callback: IDownloadManager.Callback): Boolean {
        val put = _callbackHolder.put(callback, "")
        if (put == null) {
            if (config.isDebug) {
                Log.i(IDownloadManager.TAG, "addCallback:${callback} size:${_callbackHolder.size}")
            }
        }
        return true
    }

    @Synchronized
    override fun removeCallback(callback: IDownloadManager.Callback) {
        val remove = _callbackHolder.remove(callback)
        if (remove != null) {
            if (config.isDebug) {
                Log.i(IDownloadManager.TAG, "removeCallback:${callback} size:${_callbackHolder.size}")
            }
        }
    }

    @Synchronized
    override fun addUrlCallback(url: String?, callback: IDownloadManager.Callback): Boolean {
        if (url == null || url.isEmpty()) return false
        val isDownloading = _mapDownloadInfo.containsKey(url)
        return if (isDownloading) addCallback(callback) else false
    }

    override fun getDownloadFile(url: String?): File? {
        return _downloadDirectory.getFile(url)
    }

    override fun getTempFile(url: String?): File? {
        return _downloadDirectory.getTempFile(url)
    }

    override fun deleteDownloadFile(ext: String?) {
        val count = _downloadDirectory.deleteFile(ext)
        if (config.isDebug) {
            Log.i(IDownloadManager.TAG, "deleteDownloadFile count:${count} ext:${ext}")
        }
    }

    override fun deleteTempFile() {
        val count = _downloadDirectory.deleteTempFile(object : FileInterceptor {
            override fun intercept(file: File): Boolean {
                return _mapTempFile.containsKey(file)
            }
        })
        if (config.isDebug) {
            Log.i(IDownloadManager.TAG, "deleteTempFile count:${count}")
        }
    }

    override fun getDownloadInfo(url: String?): DownloadInfo? {
        val wrapper = _mapDownloadInfo[url] ?: return null
        return wrapper.downloadInfo
    }

    override fun addTask(url: String?): Boolean {
        val downloadRequest = DownloadRequest.url(url)
        return addTask(downloadRequest)
    }

    @Synchronized
    override fun addTask(request: DownloadRequest): Boolean {
        val url = request.url
        if (url == null || url.isEmpty()) return false

        val isDownloading = _mapDownloadInfo.containsKey(url)
        if (isDownloading) return true

        val info = DownloadInfo(url)
        val tempFile = _downloadDirectory.newUrlTempFile(url)
        if (tempFile == null) {
            if (config.isDebug) {
                Log.e(IDownloadManager.TAG, "addTask error create temp file failed:${url}")
            }
            notifyError(info, DownloadError.CreateTempFile)
            return false
        }

        val downloadUpdater = InternalDownloadUpdater(info, tempFile)
        val submitted = config.downloadExecutor.submit(request, tempFile, downloadUpdater)
        if (!submitted) {
            if (config.isDebug) {
                Log.e(IDownloadManager.TAG, "addTask error submit request failed:${url}")
            }
            notifyError(info, DownloadError.SubmitFailed)
            return false
        }

        _mapDownloadInfo[url] = DownloadInfoWrapper(info, tempFile)
        _mapTempFile[tempFile] = url
        if (config.isDebug) {
            Log.i(
                IDownloadManager.TAG, "addTask url:${url} temp:${tempFile.absolutePath}" +
                        " size:${_mapDownloadInfo.size} tempSize:${_mapTempFile.size}"
            )
        }
        notifyPrepare(info)
        return true
    }

    @Synchronized
    override fun cancelTask(url: String?): Boolean {
        if (url == null || url.isEmpty()) return false
        val isDownloading = _mapDownloadInfo.containsKey(url)
        if (isDownloading) {
            if (config.isDebug) {
                Log.i(IDownloadManager.TAG, "cancelTask start url:${url}")
            }
        }

        val result = config.downloadExecutor.cancel(url)

        if (isDownloading) {
            if (config.isDebug) {
                Log.i(IDownloadManager.TAG, "cancelTask finish result:${result} url:${url}")
            }
        }
        return result
    }

    /**
     * 任务结束，移除下载信息
     */
    @Synchronized
    private fun removeDownloadInfo(url: String): DownloadInfoWrapper? {
        val wrapper = _mapDownloadInfo.remove(url)
        if (wrapper != null) {
            _mapTempFile.remove(wrapper.tempFile)
            if (config.isDebug) {
                Log.i(
                    IDownloadManager.TAG, "removeDownloadInfo url:${url}" +
                            " size:${_mapDownloadInfo.size} tempSize:" + _mapTempFile.size
                )
            }
        }
        return wrapper
    }

    private fun notifyPrepare(info: DownloadInfo) {
        info.notifyPrepare()
        val copyInfo = info.copy()
        Utils.postMainThread {
            for (item in _callbackHolder.keys) {
                item.onPrepare(copyInfo)
            }
        }
    }

    private fun notifyProgress(info: DownloadInfo, total: Long, current: Long) {
        val changed = info.notifyDownloading(total, current)
        if (!changed) return

        val copyInfo = info.copy()
        Utils.postMainThread {
            for (item in _callbackHolder.keys) {
                item.onProgress(copyInfo)
            }
        }
    }

    private fun notifySuccess(info: DownloadInfo, file: File) {
        info.notifySuccess()
        val copyInfo = info.copy()
        Utils.postMainThread {
            if (config.isDebug) {
                Log.i(IDownloadManager.TAG, "notify callback onSuccess url:${copyInfo.url} file:${file.absolutePath}")
            }
            synchronized(this@FDownloadManager) {
                removeDownloadInfo(copyInfo.url)
                for (item in _callbackHolder.keys) {
                    item.onSuccess(copyInfo, file)
                }
            }
        }
    }

    @Synchronized
    private fun notifyError(info: DownloadInfo, error: DownloadError, throwable: Throwable? = null) {
        // 立即移除下载信息，避免重新开始任务无效
        removeDownloadInfo(info.url)

        info.notifyError(error, throwable)
        val copyInfo = info.copy()
        val callbacks = _callbackHolder.keys.toTypedArray()
        Utils.postMainThread {
            if (config.isDebug) {
                Log.i(IDownloadManager.TAG, "notify callback onError url:${copyInfo.url} error:${copyInfo.error}")
            }
            for (item in callbacks) {
                item.onError(copyInfo)
            }
        }
    }

    private inner class InternalDownloadUpdater(info: DownloadInfo?, tempFile: File?) : IDownloadUpdater {
        private val mInfo: DownloadInfo
        private val mTempFile: File
        private val mUrl: String

        @Volatile
        private var mCompleted = false
        override fun notifyProgress(total: Long, current: Long) {
            if (mCompleted) return
            this@FDownloadManager.notifyProgress(mInfo, total, current)
        }

        override fun notifySuccess() {
            if (mCompleted) return
            mCompleted = true
            if (config.isDebug) Log.i(IDownloadManager.TAG, IDownloadUpdater::class.java.simpleName + " download success:" + mUrl)
            if (!mTempFile.exists()) {
                if (config.isDebug) Log.e(IDownloadManager.TAG, IDownloadUpdater::class.java.simpleName + " download success error temp file not exists:" + mUrl)
                this@FDownloadManager.notifyError(mInfo, DownloadError.TempFileNotExists)
                return
            }
            val downloadFile = _downloadDirectory.newUrlFile(mUrl)
            if (downloadFile == null) {
                if (config.isDebug) Log.e(IDownloadManager.TAG, IDownloadUpdater::class.java.simpleName + " download success error create download file:" + mUrl)
                this@FDownloadManager.notifyError(mInfo, DownloadError.CreateDownloadFile)
                return
            }
            if (downloadFile.exists()) downloadFile.delete()
            if (mTempFile.renameTo(downloadFile)) {
                this@FDownloadManager.notifySuccess(mInfo, downloadFile)
            } else {
                if (config.isDebug) Log.e(IDownloadManager.TAG, IDownloadUpdater::class.java.simpleName + " download success error rename temp file to download file:" + mUrl)
                this@FDownloadManager.notifyError(mInfo, DownloadError.RenameFile)
            }
        }

        override fun notifyError(e: Exception) {
            if (mCompleted) return
            mCompleted = true
            if (config.isDebug) Log.e(IDownloadManager.TAG, IDownloadUpdater::class.java.simpleName + " download error:" + mUrl + " " + e)
            var error = DownloadError.Other
            if (e is DownloadHttpException) {
                error = DownloadError.Http
            }
            this@FDownloadManager.notifyError(mInfo, error, e)
        }

        override fun notifyCancel() {
            if (mCompleted) return
            mCompleted = true
            if (config.isDebug) Log.i(IDownloadManager.TAG, IDownloadUpdater::class.java.simpleName + " download cancel:" + mUrl)
            this@FDownloadManager.notifyError(mInfo, DownloadError.Cancel)
        }

        init {
            requireNotNull(info) { "info is null for updater" }
            requireNotNull(tempFile) { "tempFile is null for updater" }
            mInfo = info
            mTempFile = tempFile
            mUrl = info.url
        }
    }

    companion object {
        @JvmStatic
        val default: FDownloadManager by lazy {
            val directory = config.downloadDirectory
            FDownloadManager(directory)
        }

        @JvmStatic
        private val config: DownloadManagerConfig
            get() = DownloadManagerConfig.get()
    }
}

private class DownloadInfoWrapper {
    val downloadInfo: DownloadInfo
    val tempFile: File

    constructor(downloadInfo: DownloadInfo, tempFile: File) {
        this.downloadInfo = downloadInfo
        this.tempFile = tempFile
    }
}