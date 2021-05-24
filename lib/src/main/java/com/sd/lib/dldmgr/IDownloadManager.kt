package com.sd.lib.dldmgr

import java.io.File

interface IDownloadManager {
    /**
     * 添加回调对象，可以监听所有的下载任务
     *
     * @return true-添加成功或者已添加；false-添加失败
     */
    fun addCallback(callback: Callback): Boolean

    /**
     * 移除回调对象
     */
    fun removeCallback(callback: Callback)

    /**
     * 添加回调对象，监听指定[url]的任务，如果任务不存在则回调对象不会被添加
     *
     * 如果添加成功，则任务结束之后会自动移除回调对象
     *
     * @return true-添加成功或者已添加；false-添加失败
     */
    fun addUrlCallback(url: String?, callback: Callback): Boolean

    /**
     * 返回[url]对应的文件
     *
     * @return null-文件不存在；不为null-下载文件存在
     */
    fun getDownloadFile(url: String?): File?

    /**
     * 返回[url]对应的缓存文件
     *
     * @return null-文件不存在；不为null-缓存文件存在
     */
    fun getTempFile(url: String?): File?

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
     * 返回[url]对应的下载信息
     */
    fun getDownloadInfo(url: String?): DownloadInfo?

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
         * 准备下载（已提交未开始）
         */
        fun onPrepare(info: DownloadInfo)

        /**
         * 下载中
         */
        fun onProgress(info: DownloadInfo)

        /**
         * 下载成功
         *
         * @param file 下载文件
         */
        fun onSuccess(info: DownloadInfo, file: File)

        /**
         * 下载失败
         */
        fun onError(info: DownloadInfo)
    }

    abstract class CallbackAdapter : Callback {
        override fun onPrepare(info: DownloadInfo) {}

        override fun onProgress(info: DownloadInfo) {}

        override fun onSuccess(info: DownloadInfo, file: File) {}

        override fun onError(info: DownloadInfo) {}
    }

    companion object {
        @JvmField
        val TAG = IDownloadManager::class.java.name
    }
}