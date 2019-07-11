package com.sd.lib.dldmgr;

public enum DownloadState
{
    Prepare,
    Downloading,
    Success,
    Error;

    /**
     * 是否处于活动状态，{@link #Prepare}或者{@link #Downloading}
     *
     * @return
     */
    public boolean isActive()
    {
        return this == Prepare || this == Downloading;
    }
}
