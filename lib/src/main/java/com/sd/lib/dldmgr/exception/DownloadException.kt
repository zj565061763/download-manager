package com.sd.lib.dldmgr.exception

open class DownloadException : Exception {
    @JvmOverloads
    constructor(message: String? = "", cause: Throwable? = null) : super(message, cause)

    override fun toString(): String {
        val superMessage = localizedMessage ?: ""
        val causeMessage = cause?.toString() ?: ""
        return superMessage + causeMessage
    }

    companion object {
        @JvmStatic
        fun wrap(e: Exception): DownloadException {
            return if (e is DownloadException) e else DownloadException(null, e)
        }
    }
}