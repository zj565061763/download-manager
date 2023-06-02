package com.sd.lib.dldmgr.utils

import android.webkit.MimeTypeMap

private const val Dot = "."

/**
 * 把当前字符串重命名为[name]，
 * 如果[name]包含扩展名则返回[name]，
 * 如果[name]不包含扩展名则返回[name].原扩展名
 */
internal fun String.fExtRename(name: String?): String {
    if (name.isNullOrEmpty()) return this
    if (name.fExt().isNotEmpty()) return name
    return name + this.fExt().fExtAddDot()
}

/**
 * 获取扩展名，不包括"."（例如mp3），
 * 如果未获取到到扩展名，则返回[defaultExt]，如果[defaultExt]包括"."则会移除“.”后返回
 */
internal fun String.fExt(defaultExt: String = ""): String {
    if (this.isEmpty()) return defaultExt.fExtRemoveDot()

    var ext = MimeTypeMap.getFileExtensionFromUrl(this)
    if (ext.isNullOrEmpty()) {
        ext = this.substringAfterLast(delimiter = Dot, missingDelimiterValue = "")
    }

    return if (ext.isEmpty()) {
        defaultExt.fExtRemoveDot()
    } else {
        ext.fExtRemoveDot()
    }
}

/**
 * mp3 -> .mp3
 */
internal fun String.fExtAddDot(): String {
    if (this.isEmpty()) return ""
    return if (this.startsWith(Dot)) this else "${Dot}${this}"
}

/**
 * .mp3 -> mp3
 */
internal fun String.fExtRemoveDot(): String {
    if (this.isEmpty()) return ""
    var ret = this
    while (ret.startsWith(Dot)) {
        ret = ret.removePrefix(Dot)
    }
    return ret
}