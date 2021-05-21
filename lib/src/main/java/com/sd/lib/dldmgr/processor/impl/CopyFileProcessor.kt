package com.sd.lib.dldmgr.processor.impl;

import com.sd.lib.dldmgr.IDownloadDirectory;

import java.io.File;

/**
 * 拷贝文件处理器
 */
public class CopyFileProcessor extends BaseFileProcessor
{
    public CopyFileProcessor(IDownloadDirectory directory)
    {
        super(directory);
    }

    @Override
    public void process(File file)
    {
        mDirectory.copyFile(file);
    }
}