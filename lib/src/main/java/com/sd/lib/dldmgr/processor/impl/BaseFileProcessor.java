package com.sd.lib.dldmgr.processor.impl;

import com.sd.lib.dldmgr.IDownloadDirectory;
import com.sd.lib.dldmgr.processor.IFileProcessor;

/**
 * 文件处理器
 */
public abstract class BaseFileProcessor implements IFileProcessor
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