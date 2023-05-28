package com.sd.lib.dldmgr.exception

class DownloadHttpException @JvmOverloads constructor(
    message: String? = "",
    cause: Throwable?,
) : DownloadException(message, cause)