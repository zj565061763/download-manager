package com.sd.lib.dldmgr.executor;

import com.sd.lib.dldmgr.updater.DownloadUpdater;

import java.io.File;

public interface DownloadExecutor
{
    /**
     * 提交下载任务
     *
     * @param updater
     * @param file
     * @return true-提交成功，false-提交失败
     */
    boolean submit(DownloadUpdater updater, File file);
}
