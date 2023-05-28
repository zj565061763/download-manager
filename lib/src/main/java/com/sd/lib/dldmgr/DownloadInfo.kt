package com.sd.lib.dldmgr

class DownloadInfo internal constructor(val url: String) {

    @Volatile
    var state: DownloadState = DownloadState.None
        private set

    var error: DownloadError? = null
        private set

    var throwable: Throwable? = null
        private set

    var transmitParam = TransmitParam()
        private set

    /**
     * 准备状态
     */
    internal fun notifyPrepare() {
        assert(state == DownloadState.None)
        state = DownloadState.Prepare
    }

    /**
     * 下载中
     */
    internal fun notifyDownloading(total: Long, current: Long): Boolean {
        assert(state != DownloadState.Success && state != DownloadState.Error)
        state = DownloadState.Downloading
        return transmitParam.transmit(total, current)
    }

    /**
     * 下载成功
     */
    internal fun notifySuccess() {
        assert(state != DownloadState.Success && state != DownloadState.Error)
        state = DownloadState.Success
    }

    /**
     * 下载失败
     */
    internal fun notifyError(error: DownloadError, throwable: Throwable?) {
        assert(state != DownloadState.Success && state != DownloadState.Error)
        state = DownloadState.Error
        this.error = error
        this.throwable = throwable
    }

    /**
     * 拷贝对象
     */
    internal fun copy(): DownloadInfo {
        return DownloadInfo(url).apply {
            this.state = this@DownloadInfo.state
            this.error = this@DownloadInfo.error
            this.throwable = this@DownloadInfo.throwable
            this.transmitParam = this@DownloadInfo.transmitParam.copy()
        }
    }
}