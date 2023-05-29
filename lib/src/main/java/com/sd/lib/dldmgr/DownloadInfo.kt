package com.sd.lib.dldmgr

import com.sd.lib.dldmgr.exception.DownloadException

class DownloadInfo internal constructor(val url: String) {

    @Volatile
    var state: DownloadState = DownloadState.Initialized
        private set

    var exception: DownloadException? = null
        private set

    var transmitParam = TransmitParam()
        private set

    /**
     * 下载中
     */
    internal fun notifyDownloading(total: Long, current: Long): Boolean {
        if (state.isFinished) return false
        state = DownloadState.Downloading
        return transmitParam.transmit(total, current)
    }

    /**
     * 下载成功
     */
    internal fun notifySuccess() {
        if (state.isFinished) return
        this.state = DownloadState.Success
    }

    /**
     * 下载失败
     */
    internal fun notifyError(exception: DownloadException) {
        if (state.isFinished) return
        state = DownloadState.Error
        this.exception = exception
    }

    /**
     * 拷贝对象
     */
    internal fun copy(): DownloadInfo {
        return DownloadInfo(url).apply {
            this.state = this@DownloadInfo.state
            this.exception = this@DownloadInfo.exception
            this.transmitParam = this@DownloadInfo.transmitParam.copy()
        }
    }
}