package com.sd.lib.dldmgr;

import java.io.File;

public interface DownloadManager
{
    String TAG = DownloadManager.class.getName();

    /**
     * 添加回调
     *
     * @param callback
     */
    void addCallback(Callback callback);

    /**
     * 移除回调
     *
     * @param callback
     */
    void removeCallback(Callback callback);

    /**
     * 返回url对应的文件
     *
     * @param url
     * @return
     */
    File getDownloadFile(String url);

    /**
     * 返回下载信息
     *
     * @param url
     * @return
     */
    DownloadInfo getDownloadInfo(String url);

    /**
     * 添加下载任务
     *
     * @param url
     * @return
     */
    boolean addTask(String url);

    interface Callback
    {
        void onPrepare(DownloadInfo info);

        void onProgress(DownloadInfo info);

        void onSuccess(DownloadInfo info, File file);

        void onError(DownloadInfo info);
    }
}
