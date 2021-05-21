package com.sd.lib.dldmgr

interface IDownloadUpdater {
    /**
     * 通知下载进度
     *
     * @param total   总量
     * @param current 当前传输的数量
     */
    fun notifyProgress(total: Long, current: Long)

    /**
     * 通知下载成功
     */
    fun notifySuccess()

    /**
     * 通知下载错误
     */
    fun notifyError(e: Exception)

    /**
     * 通知下载被取消
     */
    fun notifyCancel()
}