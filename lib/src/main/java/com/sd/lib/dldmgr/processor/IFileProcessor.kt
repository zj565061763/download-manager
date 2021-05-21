package com.sd.lib.dldmgr.processor

import java.io.File

/**
 * 文件处理器
 */
interface IFileProcessor {
    fun process(file: File)
}