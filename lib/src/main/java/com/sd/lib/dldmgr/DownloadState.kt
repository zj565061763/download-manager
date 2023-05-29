package com.sd.lib.dldmgr

enum class DownloadState {
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