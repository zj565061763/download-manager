package com.sd.lib.dldmgr.updater;

public interface DownloadUpdater
{
    /**
     * 通知下载进度
     *
     * @param total   总量
     * @param current 当前传输的数量
     */
    void notifyProgress(long total, long current);

    /**
     * 通知下载成功
     */
    void notifySuccess();

    /**
     * 通知下载错误
     *
     * @param e
     * @param details
     */
    void notifyError(Exception e, String details);

    /**
     * 通知下载被取消
     */
    void notifyCancel();
}
