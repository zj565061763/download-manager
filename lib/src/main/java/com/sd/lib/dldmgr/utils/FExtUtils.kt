package com.sd.lib.dldmgr.utils

import android.webkit.MimeTypeMap

/**
 * 获取扩展名，不包括"."，
 * 例如：png
 */
@JvmOverloads
internal fun String?.fGetExt(defaultExt: String? = null): String {
    if (this.isNullOrEmpty()) {
        return formatDefaultExt(defaultExt)
    }

    var ext = MimeTypeMap.getFileExtensionFromUrl(this)
    if (ext.isNullOrEmpty()) {
        ext = this.substringAfterLast(delimiter = ".", missingDelimiterValue = "")
    }

    return if (ext.isEmpty()) {
        formatDefaultExt(defaultExt)
    } else {
        removePrefixDot(ext)
    }
}

/**
 * 包含"."的完整扩展名，
 * 例如：png -> .png
 */
internal fun String?.fDotExt(): String {
    return if (this.isNullOrEmpty()) {
        ""
    } else {
        if (this.startsWith(".")) this else ".$this"
    }
}

private fun formatDefaultExt(defaultExt: String?): String {
    return if (defaultExt.isNullOrEmpty()) {
        ""
    } else {
        removePrefixDot(defaultExt)
    }
}

internal fun removePrefixDot(input: String): String {
    var ret = input
    while (ret.startsWith(".")) {
        ret = ret.removePrefix(".")
    }
    return ret
}