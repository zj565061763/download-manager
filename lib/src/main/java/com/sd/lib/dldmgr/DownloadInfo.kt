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
     * 下载进度
     */
    internal fun notifyProgress(total: Long, current: Long): Boolean {
        if (state.isFinished) return false
        state = DownloadState.Downloading
        return transmitParam.transmit(total, current)
    }

    /**
     * 下载成功
     */
    internal fun notifySuccess(): Boolean {
        if (!state.isFinished) return false
        this.state = DownloadState.Success
        return true
    }

    /**
     * 下载失败
     */
    internal fun notifyError(exception: DownloadException): Boolean {
        if (state.isFinished) return false
        state = DownloadState.Error
        this.exception = exception
        return true
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