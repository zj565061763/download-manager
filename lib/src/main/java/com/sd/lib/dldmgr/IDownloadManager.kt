package com.sd.lib.dldmgr

import com.sd.lib.dldmgr.exception.DownloadException
import java.io.File

interface IDownloadManager {
    /**
     * 添加回调对象，可以监听所有的下载任务
     */
    fun addCallback(callback: Callback)

    /**
     * 移除回调对象
     */
    fun removeCallback(callback: Callback)

    /**
     * 返回[url]对应的文件
     *
     * @return null-文件不存在；不为null-下载文件存在
     */
    fun getDownloadFile(url: String?): File?

    /**
     * 删除下载文件（临时文件不会被删除）
     *
     * 如果指定了扩展名，则扩展名不能包含点符号
     *
     * 合法：mp3  不合法：.mp3
     *
     * @param ext 文件扩展名(例如mp3)；null-所有下载文件；空字符串-删除扩展名为空的文件
     */
    fun deleteDownloadFile(ext: String?)

    /**
     * 删除所有临时文件（下载中的临时文件不会被删除）
     */
    fun deleteTempFile()

    /**
     * 是否有[url]对应的下载任务
     */
    fun hasTask(url: String?): Boolean

    /**
     * 添加下载任务
     *
     * @return true-任务添加成功或者已经添加
     */
    fun addTask(url: String?): Boolean

    /**
     * 添加下载任务
     *
     * @return true-任务添加成功或者已经添加
     */
    fun addTask(request: DownloadRequest): Boolean

    /**
     * 取消下载任务
     *
     * @return true-任务被取消
     */
    fun cancelTask(url: String?): Boolean

    /**
     * 监听任务
     */
    suspend fun awaitTask(url: String, callback: Callback? = null): File?

    /**
     * 下载回调
     */
    interface Callback {
        /**
         * 下载中
         */
        fun onProgress(url: String, progress: DownloadProgress)

        /**
         * 下载成功
         */
        fun onSuccess(url: String, file: File)

        /**
         * 下载失败
         */
        fun onError(url: String, exception: DownloadException)
    }
}