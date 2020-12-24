package com.sd.lib.dldmgr;

import java.io.File;

public interface IDownloadManager
{
    String TAG = IDownloadManager.class.getName();

    /**
     * 添加回调对象
     *
     * @param callback
     * @return true-添加成功或者已添加；false-添加失败
     */
    boolean addCallback(Callback callback);

    /**
     * 移除回调对象
     *
     * @param callback
     */
    void removeCallback(Callback callback);

    /**
     * 添加回调对象
     * <p>
     * 指定的url任务存在的时候，回调对象才会被添加
     *
     * @param url
     * @param callback
     * @return true-添加成功或者已添加；false-添加失败
     */
    boolean addUrlCallback(String url, Callback callback);

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
     * 添加url对应的文件处理器，只有url正在下载的时候，处理器对象才会被添加
     * <p>
     * 下载成功之后，会把文件传给处理器处理（后台线程），处理完毕之后，处理器对象会被移除
     *
     * @param url
     * @param processor
     * @return true-添加成功；false-添加失败
     */
    boolean addFileProcessor(String url, FileProcessor processor);

    /**
     * 移除url对应的文件处理器
     *
     * @param url
     * @param processor
     */
    void removeFileProcessor(String url, FileProcessor processor);

    /**
     * 清空url对应的文件处理器
     *
     * @param url
     */
    void clearFileProcessor(String url);

    /**
     * {@link #addTask(DownloadRequest)}
     *
     * @param url
     * @return
     */
    boolean addTask(String url);

    /**
     * 添加下载任务
     *
     * @param request 下载请求
     * @return true-任务添加成功或者已经添加
     */
    boolean addTask(DownloadRequest request);

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

    /**
     * 文件处理器
     */
    interface FileProcessor
    {
        void process(File file);
    }
}
