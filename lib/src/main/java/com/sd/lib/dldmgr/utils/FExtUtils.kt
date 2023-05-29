package com.sd.lib.dldmgr.utils

import android.webkit.MimeTypeMap

private const val Dot = "."

/**
 * 获取扩展名，不包括"."（例如mp3），
 * 如果未获取到到扩展名，则返回[defaultExt]，如果[defaultExt]包括"."则会移除“.”后返回
 */
@JvmOverloads
internal fun String.fGetExt(defaultExt: String = ""): String {
    if (this.isEmpty()) return defaultExt.fNoneDotExt()

    var ext = MimeTypeMap.getFileExtensionFromUrl(this)
    if (ext.isNullOrEmpty()) {
        ext = this.substringAfterLast(delimiter = Dot, missingDelimiterValue = "")
    }

    return if (ext.isEmpty()) {
        defaultExt.fNoneDotExt()
    } else {
        ext.fNoneDotExt()
    }
}

/**
 * mp3 -> .mp3
 */
internal fun String.fDotExt(): String {
    if (this.isEmpty()) return ""
    return if (this.startsWith(Dot)) this else "${Dot}${this}"
}

/**
 * .mp3 -> mp3
 */
internal fun String.fNoneDotExt(): String {
    if (this.isEmpty()) return ""
    var ret = this
    while (ret.startsWith(Dot)) {
        ret = ret.removePrefix(Dot)
    }
    return ret
}