package com.sd.lib.dldmgr;

import java.io.File;

public interface IDownloadManager
{
    String TAG = IDownloadManager.class.getName();

    /**
     * 添加回调对象
     *
     * @param callback
     */
    void addCallback(Callback callback);

    /**
     * 移除回调对象
     *
     * @param callback
     */
    void removeCallback(Callback callback);

    /**
     * 返回url对应的文件
     *
     * @param url
     * @return null-文件不存在，不为null下载文件存在
     */
    File getDownloadFile(String url);

    /**
     * 返回url对应的缓存文件
     *
     * @param url
     * @return null-文件不存在，不为null缓存文件存在
     */
    File getTempFile(String url);

    /**
     * 返回下载信息
     *
     * @param url
     * @return
     */
    DownloadInfo getDownloadInfo(String url);

    /**
     * 删除所有临时文件（下载中的临时文件不会被删除）
     */
    void deleteTempFile();

    /**
     * 删除下载文件（临时文件不会被删除）
     * <p>
     * 如果指定了扩展名，则扩展名不能包含点符号：<br>
     * 合法：mp3<br>
     * 不合法：.mp3
     *
     * @param ext 文件扩展名(例如mp3)；null-所有下载文件；空字符串-删除扩展名为空的文件
     */
    void deleteDownloadFile(String ext);

    /**
     * 设置url下载成功之后，要拷贝到哪个下载目录
     *
     * @param url
     * @param directory
     * @return
     */
    boolean addDownloadDirectory(String url, IDownloadDirectory directory);

    /**
     * {@link #addTask(DownloadRequest, Callback)}
     *
     * @param url
     * @return
     */
    boolean addTask(String url);

    /**
     * {@link #addTask(DownloadRequest, Callback)}
     *
     * @param request
     * @return
     */
    boolean addTask(DownloadRequest request);

    /**
     * 添加下载任务
     *
     * @param request
     * @param callback 只有任务添加成功或者已经添加的情况下，回调对象才会被添加
     * @return true-任务添加成功或者已经添加
     */
    boolean addTask(DownloadRequest request, Callback callback);

    /**
     * 取消下载任务
     *
     * @param url
     * @return true-任务被取消
     */
    boolean cancelTask(String url);

    /**
     * 下载回调
     */
    interface Callback
    {
        void onPrepare(DownloadInfo info);

        void onProgress(DownloadInfo info);

        void onSuccess(DownloadInfo info, File file);

        void onError(DownloadInfo info);
    }
}
