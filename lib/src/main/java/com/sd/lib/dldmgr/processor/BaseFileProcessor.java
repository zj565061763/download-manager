package com.sd.lib.dldmgr.processor;

import com.sd.lib.dldmgr.IDownloadDirectory;
import com.sd.lib.dldmgr.IDownloadManager;

/**
 * 文件处理器
 */
public abstract class BaseFileProcessor implements IDownloadManager.FileProcessor
{
    protected final IDownloadDirectory mDirectory;

    public BaseFileProcessor(IDownloadDirectory directory)
    {
        if (directory == null)
            throw new NullPointerException("directory is null");
        mDirectory = directory;
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

        final BaseFileProcessor other = (BaseFileProcessor) obj;
        return mDirectory.equals(other.mDirectory);
    }
}