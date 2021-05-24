package com.sd.lib.dldmgr

import android.util.Log
import com.sd.lib.dldmgr.directory.DownloadDirectory
import com.sd.lib.dldmgr.directory.IDownloadDirectory.FileInterceptor
import com.sd.lib.dldmgr.exception.DownloadException
import com.sd.lib.dldmgr.exception.DownloadHttpException
import com.sd.lib.dldmgr.utils.UrlCallbackHolder
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FDownloadManager : IDownloadManager {
    private val _downloadDirectory: DownloadDirectory

    private val _mapDownloadInfo: MutableMap<String, DownloadInfoWrapper> = ConcurrentHashMap()
    private val _mapTempFile: MutableMap<File, String> = ConcurrentHashMap()

    private val _callbackHolder: MutableMap<IDownloadManager.Callback, String> = ConcurrentHashMap()
    private val _urlCallbackHolder = UrlCallbackHolder()

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
        if (_callbackHolder.remove(callback) != null) {
            if (config.isDebug) {
                Log.i(IDownloadManager.TAG, "removeCallback:${callback} size:${_callbackHolder.size}")
            }
        }
        _urlCallbackHolder.remove(callback)
    }

    @Synchronized
    override fun addUrlCallback(url: String?, callback: IDownloadManager.Callback): Boolean {
        if (url == null || url.isEmpty()) return false
        val hasTask = _mapDownloadInfo.containsKey(url)
        if (!hasTask) return false

        _urlCallbackHolder.add(url, callback)
        return true
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

        val downloadInfo = getDownloadInfo(url)
        val isDownloading = downloadInfo != null && !downloadInfo.state.isCompleted
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

    override suspend fun awaitTask(url: String, callback: IDownloadManager.Callback?): File? {
        return suspendCoroutine { continuation ->
            val add = addUrlCallback(url, object : IDownloadManager.Callback {
                override fun onPrepare(info: DownloadInfo) {
                    callback?.onPrepare(info)
                }

                override fun onProgress(info: DownloadInfo) {
                    callback?.onProgress(info)
                }

                override fun onSuccess(info: DownloadInfo, file: File) {
                    callback?.onSuccess(info, file)
                    continuation.resume(file)
                }

                override fun onError(info: DownloadInfo) {
                    callback?.onError(info)
                    continuation.resume(null)
                }
            })
            if (add) {
                // 等待任务完成
            } else {
                continuation.resume(null)
            }
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

            _urlCallbackHolder.getUrl(copyInfo.url)?.let {
                for (item in it) {
                    item.onPrepare(copyInfo)
                }
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

            _urlCallbackHolder.getUrl(copyInfo.url)?.let {
                for (item in it) {
                    item.onProgress(copyInfo)
                }
            }
        }
    }

    private fun notifySuccess(info: DownloadInfo, file: File) {
        info.notifySuccess()
        val copyInfo = info.copy()
        Utils.postMainThread {
            synchronized(this@FDownloadManager) {
                removeDownloadInfo(copyInfo.url)
                if (config.isDebug) {
                    Log.i(IDownloadManager.TAG, "notify callback onSuccess url:${copyInfo.url} file:${file.absolutePath}")
                }
                for (item in _callbackHolder.keys) {
                    item.onSuccess(copyInfo, file)
                }

                _urlCallbackHolder.removeUrl(copyInfo.url)?.let {
                    for (item in it) {
                        item.onSuccess(copyInfo, file)
                    }
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
        val urlCallbacks = _urlCallbackHolder.removeUrl(copyInfo.url)
        Utils.postMainThread {
            if (config.isDebug) {
                Log.i(IDownloadManager.TAG, "notify callback onError url:${copyInfo.url} error:${copyInfo.error}")
            }
            for (item in callbacks) {
                item.onError(copyInfo)
            }

            urlCallbacks?.let {
                for (item in it) {
                    item.onError(copyInfo)
                }
            }
        }
    }

    private inner class InternalDownloadUpdater : IDownloadUpdater {
        private val _iUrl: String
        private val _iDownloadInfo: DownloadInfo
        private val _iTempFile: File

        @Volatile
        private var _iCompleted = false

        constructor(info: DownloadInfo, tempFile: File) {
            _iUrl = info.url
            _iDownloadInfo = info
            _iTempFile = tempFile
        }

        override fun notifyProgress(total: Long, current: Long) {
            if (_iCompleted) return
            this@FDownloadManager.notifyProgress(_iDownloadInfo, total, current)
        }

        override fun notifySuccess() {
            if (_iCompleted) return
            _iCompleted = true

            if (config.isDebug) {
                Log.i(
                    IDownloadManager.TAG,
                    "${IDownloadUpdater::class.java.simpleName} download success ${_iUrl}"
                )
            }

            if (!_iTempFile.exists()) {
                if (config.isDebug) {
                    Log.e(
                        IDownloadManager.TAG,
                        "${IDownloadUpdater::class.java.simpleName} download success error temp file not exists ${_iUrl}"
                    )
                }
                this@FDownloadManager.notifyError(_iDownloadInfo, DownloadError.TempFileNotExists)
                return
            }

            val downloadFile = _downloadDirectory.newUrlFile(_iUrl)
            if (downloadFile == null) {
                if (config.isDebug) {
                    Log.e(
                        IDownloadManager.TAG,
                        "${IDownloadUpdater::class.java.simpleName} download success error create download file ${_iUrl}"
                    )
                }
                this@FDownloadManager.notifyError(_iDownloadInfo, DownloadError.CreateDownloadFile)
                return
            }

            if (Utils.moveFile(_iTempFile, downloadFile)) {
                this@FDownloadManager.notifySuccess(_iDownloadInfo, downloadFile)
            } else {
                if (config.isDebug) Log.e(
                    IDownloadManager.TAG,
                    "${IDownloadUpdater::class.java.simpleName} download success error rename temp file to download file ${_iUrl}"
                )
                this@FDownloadManager.notifyError(_iDownloadInfo, DownloadError.RenameFile)
            }
        }

        override fun notifyError(e: Exception) {
            if (_iCompleted) return
            _iCompleted = true

            if (config.isDebug) Log.e(
                IDownloadManager.TAG,
                "${IDownloadUpdater::class.java.simpleName} download error:${e} ${_iUrl}"
            )

            var error = DownloadError.Other
            if (e is DownloadHttpException) {
                error = DownloadError.Http
            }
            this@FDownloadManager.notifyError(_iDownloadInfo, error, DownloadException.wrap(e))
        }

        override fun notifyCancel() {
            if (_iCompleted) return
            _iCompleted = true

            if (config.isDebug) {
                Log.i(IDownloadManager.TAG, "${IDownloadUpdater::class.java.simpleName} download cancel ${_iUrl}")
            }
            this@FDownloadManager.notifyError(_iDownloadInfo, DownloadError.Cancel)
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