package com.sd.lib.dldmgr.processor.impl;

import com.sd.lib.dldmgr.IDownloadDirectory;

import java.io.File;

/**
 * 移动文件处理器
 */
public class TakeFileProcessor extends BaseFileProcessor
{
    public TakeFileProcessor(IDownloadDirectory directory)
    {
        super(directory);
    }

    @Override
    public void process(File file)
    {
        mDirectory.takeFile(file);
    }
}