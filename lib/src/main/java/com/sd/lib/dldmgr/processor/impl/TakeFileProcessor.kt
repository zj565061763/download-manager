package com.sd.lib.dldmgr.processor.impl

import com.sd.lib.dldmgr.directory.IDownloadDirectory
import java.io.File

/**
 * 移动文件处理器
 */
class TakeFileProcessor : BaseFileProcessor {
    constructor(directory: IDownloadDirectory) : super(directory)

    override fun process(file: File) {
        directory.takeFile(file)
    }
}