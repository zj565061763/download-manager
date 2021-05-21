package com.sd.lib.dldmgr.exception

class DownloadHttpException : DownloadException {
    @JvmOverloads
    constructor(message: String? = "", cause: Throwable?) : super(message, cause)
}