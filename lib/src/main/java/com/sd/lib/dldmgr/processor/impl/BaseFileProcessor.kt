package com.sd.lib.dldmgr.processor.impl

import com.sd.lib.dldmgr.directory.IDownloadDirectory
import com.sd.lib.dldmgr.processor.IFileProcessor

/**
 * 文件处理器
 */
abstract class BaseFileProcessor(directory: IDownloadDirectory) : IFileProcessor {
    protected val directory: IDownloadDirectory = directory

    override fun hashCode(): Int {
        return directory.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is BaseFileProcessor) return false
        return directory == other.directory
    }
}