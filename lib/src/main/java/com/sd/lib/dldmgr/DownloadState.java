package com.sd.lib.dldmgr;

public enum DownloadState
{
    Prepare,
    Downloading,
    Success,
    Error;

    /**
     * 是否处于完成状态，{@link #Success}或者{@link #Error}
     *
     * @return
     */
    public boolean isCompleted()
    {
        return this == Success || this == Error;
    }
}
