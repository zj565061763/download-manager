package com.sd.lib.dldmgr.updater;

public interface DownloadUpdater
{
    void notifyProgress(long total, long current);

    void notifySuccess();

    void notifyError(Exception e, String details);
}
