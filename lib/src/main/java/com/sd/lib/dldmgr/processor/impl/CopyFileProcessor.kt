package com.sd.lib.dldmgr.processor.impl

import com.sd.lib.dldmgr.IDownloadDirectory
import java.io.File

/**
 * 拷贝文件处理器
 */
class CopyFileProcessor : BaseFileProcessor {
    constructor(directory: IDownloadDirectory) : super(directory)

    override fun process(file: File) {
        directory.copyFile(file)
    }
}