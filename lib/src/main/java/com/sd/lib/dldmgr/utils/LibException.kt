package com.sd.lib.dldmgr.utils

import java.io.IOException

internal fun libWhiteExceptionList(): List<Class<out Exception>> {
    return listOf(
        IOException::class.java,
        SecurityException::class.java,
    )
}

internal fun <T> Exception.libThrowOrReturn(
    whiteList: List<Class<out Exception>> = libWhiteExceptionList(),
    block: () -> T,
): T {
    if (this.javaClass in whiteList) {
        this.printStackTrace()
        return block()
    } else {
        throw this
    }
}