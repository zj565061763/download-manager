package com.sd.lib.dldmgr

data class DownloadProgress(
    /** 总数量 */
    val total: Long,

    /** 已传输数量 */
    val current: Long,

    /** 传输进度[0-100] */
    val progress: Int,

    /** 传输速率（B/S） */
    val speedBps: Int,
) {
    /** 传输速率（KB/S） */
    val speedKBps: Int
        get() = speedBps / 1024
}

internal class DownloadInfo(val url: String) {
    private var _state = DownloadState.Initialized
    private val _transmitParam = TransmitParam()

    /**
     * 下载进度
     */
    @Synchronized
    fun notifyProgress(total: Long, current: Long): DownloadProgress? {
        if (_state.isFinished) return null
        _state = DownloadState.Downloading
        return if (_transmitParam.transmit(total, current)) {
            DownloadProgress(
                total = _transmitParam.total,
                current = _transmitParam.current,
                progress = _transmitParam.progress,
                speedBps = _transmitParam.speedBps,
            )
        } else null
    }

    /**
     * 下载成功
     */
    @Synchronized
    fun notifySuccess(): Boolean {
        if (_state.isFinished) return false
        this._state = DownloadState.Success
        return true
    }

    /**
     * 下载失败
     */
    @Synchronized
    fun notifyError(): Boolean {
        if (_state.isFinished) return false
        _state = DownloadState.Error
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
    private var _lastSpeedTime: Long = 0
    private var _lastSpeedCount: Long = 0

    /** 总数量 */
    var total: Long = 0
        private set

    /** 已传输数量 */
    var current: Long = 0
        private set

    /** 传输进度[0-100] */
    var progress: Int = 0
        private set

    /** 传输速率（B/S） */
    var speedBps: Int = 0
        private set

    /**
     * 传输
     *
     * @param total 总量
     * @param current 当前传输量
     * @return true-进度发生了变化
     */
    fun transmit(total: Long, current: Long): Boolean {
        val oldProgress = progress
        if (total <= 0 || current <= 0) {
            reset()
            return oldProgress != progress
        }

        kotlin.run {
            val time = System.currentTimeMillis()
            val interval = time - _lastSpeedTime
            if (interval >= _calculateSpeedInterval) {
                val count = current - _lastSpeedCount
                speedBps = (count * (1000f / interval)).toInt().coerceAtLeast(0)
                _lastSpeedTime = time
                _lastSpeedCount = current
            }
        }

        this.total = total
        this.current = current
        this.progress = (current * 100 / total).toInt().coerceAtMost(100)

        return this.progress > oldProgress
    }

    private fun reset() {
        _lastSpeedTime = 0
        _lastSpeedCount = 0
        total = 0
        current = 0
        progress = 0
        speedBps = 0
    }

    override fun toString(): String {
        return "${current}/${total} ${progress}% ${super.toString()}"
    }
}