package com.sd.lib.dldmgr.processor;

import com.sd.lib.dldmgr.IDownloadDirectory;
import com.sd.lib.dldmgr.IDownloadManager;

import java.io.File;

/**
 * 文件拷贝处理器
 */
public class CopyFileProcessor implements IDownloadManager.FileProcessor
{
    private final IDownloadDirectory mDirectory;

    public CopyFileProcessor(IDownloadDirectory directory)
    {
        if (directory == null)
            throw new NullPointerException("directory is null");
        mDirectory = directory;
    }

    @Override
    public void process(File file)
    {
        mDirectory.copyFile(file);
    }

    @Override
    public int hashCode()
    {
        return mDirectory.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (obj == null) return false;
        if (obj.getClass() != getClass()) return false;

        final CopyFileProcessor other = (CopyFileProcessor) obj;
        return mDirectory.equals(other.mDirectory);
    }
}