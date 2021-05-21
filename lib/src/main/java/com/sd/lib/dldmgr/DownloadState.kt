package com.sd.lib.dldmgr

enum class DownloadState {
    /** 准备下载 */
    Prepare,

    /** 下载中 */
    Downloading,

    /** 下载成功 */
    Success,

    /** 下载失败 */
    Error;

    /** 是否处于完成状态，[Success]或者[Error] */
    val isCompleted: Boolean
        get() = this == Success || this == Error
}