package com.sd.lib.dldmgr.processor.impl

import com.sd.lib.dldmgr.directory.IDownloadDirectory
import java.io.File

/**
 * 拷贝文件处理器
 */
class CopyFileProcessor(directory: IDownloadDirectory) : BaseFileProcessor(directory) {
    override fun process(file: File) {
        directory.copyFile(file)
    }
}