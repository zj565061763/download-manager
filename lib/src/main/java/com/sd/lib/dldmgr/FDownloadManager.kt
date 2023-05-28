package com.sd.lib.dldmgr

import com.sd.lib.dldmgr.directory.DownloadDirectory
import com.sd.lib.dldmgr.directory.IDownloadDirectory.FileInterceptor
import com.sd.lib.dldmgr.exception.DownloadException
import com.sd.lib.dldmgr.exception.DownloadHttpException
import com.sd.lib.dldmgr.executor.IDownloadUpdater
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.suspendCoroutine

object FDownloadManager : IDownloadManager {
    private val _mapDownloadInfo: MutableMap<String, DownloadInfoWrapper> = ConcurrentHashMap()
    private val _mapTempFile: MutableMap<File, String> = ConcurrentHashMap()

    private val _callbackHolder: MutableMap<IDownloadManager.Callback, String> = ConcurrentHashMap()

    private val config get() = DownloadManagerConfig.get()
    private val _downloadDirectory by lazy { DownloadDirectory.from(config.downloadDirectory) }

    override fun addCallback(callback: IDownloadManager.Callback) {
        synchronized(this@FDownloadManager) {
            val put = _callbackHolder.put(callback, "")
            if (put == null) {
                logMsg { "addCallback:${callback} size:${_callbackHolder.size}" }
            }
        }
    }

    override fun removeCallback(callback: IDownloadManager.Callback) {
        synchronized(this@FDownloadManager) {
            if (_callbackHolder.remove(callback) != null) {
                logMsg { "removeCallback:${callback} size:${_callbackHolder.size}" }
            }
        }
    }

    override fun getDownloadFile(url: String?): File? {
        return _downloadDirectory.getFile(url)
    }

    override fun getTempFile(url: String?): File? {
        return _downloadDirectory.getTempFile(url)
    }

    override fun deleteDownloadFile(ext: String?) {
        val count = _downloadDirectory.deleteFile(ext)
        if (count > 0) {
            logMsg { "deleteDownloadFile count:${count} ext:${ext}" }
        }
    }

    override fun deleteTempFile() {
        val count = _downloadDirectory.deleteTempFile(
            object : FileInterceptor {
                override fun intercept(file: File): Boolean {
                    return _mapTempFile.containsKey(file)
                }
            }
        )
        if (count > 0) {
            logMsg { "deleteTempFile count:${count}" }
        }
    }

    override fun getDownloadInfo(url: String?): DownloadInfo? {
        val wrapper = _mapDownloadInfo[url] ?: return null
        return wrapper.downloadInfo
    }

    override fun addTask(url: String?): Boolean {
        return addTask(DownloadRequest.Builder().build(url))
    }

    @Synchronized
    override fun addTask(request: DownloadRequest): Boolean {
        val url = request.url

        val isDownloading = _mapDownloadInfo.containsKey(url)
        if (isDownloading) return true

        val info = DownloadInfo(url)
        val tempFile = _downloadDirectory.newUrlTempFile(url)
        if (tempFile == null) {
            logMsg { "addTask error create temp file failed:${url}" }
            notifyError(info, DownloadError.CreateTempFile)
            return false
        }

        val downloadUpdater = DefaultDownloadUpdater(info, tempFile, _downloadDirectory)
        val submitted = config.downloadExecutor.submit(request, tempFile, downloadUpdater)
        if (!submitted) {
            logMsg { "addTask error submit request failed:${url}" }
            notifyError(info, DownloadError.SubmitFailed)
            return false
        }

        _mapDownloadInfo[url] = DownloadInfoWrapper(info, tempFile)
        _mapTempFile[tempFile] = url
        logMsg {
            "addTask url:${url} temp:${tempFile.absolutePath} size:${_mapDownloadInfo.size} tempSize:${_mapTempFile.size}"
        }
        notifyPrepare(info)
        return true
    }

    @Synchronized
    override fun cancelTask(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false

        val downloadInfo = getDownloadInfo(url)
        val isDownloading = downloadInfo != null && !downloadInfo.state.isCompleted
        if (isDownloading) {
            logMsg { "cancelTask start url:${url}" }
        }

        val result = config.downloadExecutor.cancel(url)

        if (isDownloading) {
            logMsg { "cancelTask finish result:${result} url:${url}" }
        }
        return result
    }

    override suspend fun awaitTask(url: String, callback: IDownloadManager.Callback?): File? {
        return suspendCoroutine { continuation ->
            // TODO
//            val add = addUrlCallback(url, object : IDownloadManager.Callback {
//                override fun onPrepare(info: DownloadInfo) {
//                    callback?.onPrepare(info)
//                }
//
//                override fun onProgress(info: DownloadInfo) {
//                    callback?.onProgress(info)
//                }
//
//                override fun onSuccess(info: DownloadInfo, file: File) {
//                    callback?.onSuccess(info, file)
//                    continuation.resume(file)
//                }
//
//                override fun onError(info: DownloadInfo) {
//                    callback?.onError(info)
//                    continuation.resume(null)
//                }
//            })
//            if (add) {
//                // 等待任务完成
//            } else {
//                continuation.resume(null)
//            }
        }
    }

    /**
     * 任务结束，移除下载信息
     */
    @Synchronized
    private fun removeDownloadInfo(url: String): DownloadInfoWrapper? {
        val wrapper = _mapDownloadInfo.remove(url)
        if (wrapper != null) {
            _mapTempFile.remove(wrapper.tempFile)
            logMsg {
                "removeDownloadInfo url:${url} size:${_mapDownloadInfo.size} tempSize:${_mapTempFile.size}"
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

    internal fun notifyProgress(info: DownloadInfo, total: Long, current: Long) {
        val changed = info.notifyDownloading(total, current)
        if (!changed) return

        val copyInfo = info.copy()
        Utils.postMainThread {
            for (item in _callbackHolder.keys) {
                item.onProgress(copyInfo)
            }
        }
    }

    internal fun notifySuccess(info: DownloadInfo, file: File) {
        info.notifySuccess()
        val copyInfo = info.copy()
        Utils.postMainThread {
            synchronized(this@FDownloadManager) {
                removeDownloadInfo(copyInfo.url)
                logMsg { "notify callback onSuccess url:${copyInfo.url} file:${file.absolutePath}" }
                for (item in _callbackHolder.keys) {
                    item.onSuccess(copyInfo, file)
                }
            }
        }
    }

    @Synchronized
    internal fun notifyError(info: DownloadInfo, error: DownloadError, throwable: Throwable? = null) {
        // 立即移除下载信息，避免重新开始任务无效
        removeDownloadInfo(info.url)
        info.notifyError(error, throwable)
        val copyInfo = info.copy()

        val callbacks = _callbackHolder.keys.toTypedArray()
        Utils.postMainThread {
            logMsg { "notify callback onError url:${copyInfo.url} error:${copyInfo.error}" }
            for (item in callbacks) {
                item.onError(copyInfo)
            }
        }
    }
}

private class DefaultDownloadUpdater(
    info: DownloadInfo,
    tempFile: File,
    downloadDirectory: DownloadDirectory,
) : IDownloadUpdater {

    private val _url = info.url
    private val _downloadInfo = info
    private val _tempFile = tempFile
    private val _downloadDirectory = downloadDirectory

    @Volatile
    private var _isFinish = false
        set(value) {
            require(value) { "Require true value." }
            field = value
        }

    override fun notifyProgress(total: Long, current: Long) {
        if (_isFinish) return
        FDownloadManager.notifyProgress(_downloadInfo, total, current)
    }

    override fun notifySuccess() {
        if (_isFinish) return
        _isFinish = true
        logMsg { "updater download success $_url" }

        if (!_tempFile.exists()) {
            logMsg { "updater download success error temp file not exists $_url" }
            FDownloadManager.notifyError(_downloadInfo, DownloadError.TempFileNotExists)
            return
        }

        val downloadFile = _downloadDirectory.newUrlFile(_url)
        if (downloadFile == null) {
            logMsg { "updater download success error create download file $_url" }
            FDownloadManager.notifyError(_downloadInfo, DownloadError.CreateDownloadFile)
            return
        }

        if (Utils.moveFile(_tempFile, downloadFile)) {
            FDownloadManager.notifySuccess(_downloadInfo, downloadFile)
        } else {
            logMsg { "updater download success error rename temp file to download file $_url" }
            FDownloadManager.notifyError(_downloadInfo, DownloadError.RenameFile)
        }
    }

    override fun notifyError(t: Throwable) {
        if (_isFinish) return
        _isFinish = true
        logMsg { "updater download error:${t} $_url" }

        var error = DownloadError.Other
        if (t is DownloadHttpException) {
            error = DownloadError.Http
        }
        FDownloadManager.notifyError(_downloadInfo, error, DownloadException.wrap(t))
    }

    override fun notifyCancel() {
        if (_isFinish) return
        _isFinish = true
        logMsg { "${IDownloadUpdater::class.java.simpleName} download cancel $_url" }
        FDownloadManager.notifyError(_downloadInfo, DownloadError.Cancel)
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