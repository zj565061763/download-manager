package com.sd.lib.dldmgr

data class DownloadProgress(
    /** 总量 */
    val total: Long,

    /** 当前传输量 */
    val current: Long,

    /** 传输进度 */
    val progress: Int,

    /** 传输速率（B/S） */
    val speedBps: Int,
) {
    /** 传输速率（KB/S） */
    val speedKBps: Int
        get() = speedBps / 1024

    /** 传输是否完成 */
    val isFinished: Boolean
        get() = current > 0 && current == total
}

internal class DownloadInfo(val url: String) {
    @Volatile
    private var state: DownloadState = DownloadState.Initialized

    private val _transmitParam = TransmitParam()

    /** 下载进度 */
    val progress: DownloadProgress
        get() = DownloadProgress(
            total = _transmitParam.total,
            current = _transmitParam.current,
            progress = _transmitParam.progress,
            speedBps = _transmitParam.speedBps,
        )

    /**
     * 下载进度
     */
    fun notifyProgress(total: Long, current: Long): Boolean {
        if (state.isFinished) return false
        state = DownloadState.Downloading
        return _transmitParam.transmit(total, current)
    }

    /**
     * 下载成功
     */
    fun notifySuccess(): Boolean {
        if (state.isFinished) return false
        this.state = DownloadState.Success
        return true
    }

    /**
     * 下载失败
     */
    fun notifyError(): Boolean {
        if (state.isFinished) return false
        state = DownloadState.Error
        return true
    }
}

private enum class DownloadState {
    /** 初始状态 */
    Initialized,

    /** 下载中 */
    Downloading,

    /** 下载成功 */
    Success,

    /** 下载失败 */
    Error;

    /** 是否处于完成状态，[Success]或者[Error] */
    val isFinished: Boolean
        get() = this == Success || this == Error
}

private class TransmitParam(calculateSpeedInterval: Long = 100) {
    private val _calculateSpeedInterval: Long = calculateSpeedInterval.coerceAtLeast(100)
    private var _lastTime: Long = 0
    private var _lastCount: Long = 0

    /** 总量 */
    var total: Long = 0
        private set

    /** 当前传输量 */
    var current: Long = 0
        private set

    /** 传输进度 */
    var progress: Int = 0
        private set

    /** 传输速率（B/S） */
    var speedBps: Int = 0
        private set

    /** 传输速率（KB/S） */
    val speedKBps: Int
        get() = speedBps / 1024

    /** 传输是否完成 */
    val isFinished: Boolean
        get() = current > 0 && current == total

    /**
     * 传输
     *
     * @param total 总量
     * @param current 当前传输量
     * @return true-进度发生了变化
     */
    @Synchronized
    fun transmit(total: Long, current: Long): Boolean {
        val oldProgress = progress
        if (total <= 0 || current <= 0) {
            reset()
            return oldProgress != progress
        }

        this.total = total
        this.current = current

        val currentTime = System.currentTimeMillis()
        val interval = currentTime - _lastTime
        if (interval >= _calculateSpeedInterval) {
            val count = current - _lastCount
            speedBps = (count * (1000f / interval)).toInt()
            _lastTime = currentTime
            _lastCount = current
        }

        progress = (current * 100 / total).toInt()
        return progress > oldProgress
    }

    private fun reset() {
        total = 0
        current = 0
        progress = 0
        speedBps = 0
        _lastTime = 0
        _lastCount = 0
    }

    override fun toString(): String {
        return "${current}/${total} ${progress}% ${speedKBps}KBps ${super.toString()}"
    }
}