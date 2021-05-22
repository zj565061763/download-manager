# About
安卓下载管理<br>
* 断点下载
* 下载目录配置
* 自定义底层下载处理器，例如使用okhttp等网络请求框架
* 指定url下载监听
* 全局下载监听

# Gradle
[![](https://jitpack.io/v/zj565061763/download-manager.svg)](https://jitpack.io/#zj565061763/download-manager)

# 初始化
```java
DownloadManagerConfig.init(new DownloadManagerConfig.Builder()
        /**
         * 设置下载目录，如果不配置则默认路径为：(sd卡或者内部存储)/Android/data/包名/cache/fdownload
         */
        .setDownloadDirectory(getExternalCacheDir().getAbsolutePath())
        /**
         * 设置下载处理器，如果不配置则默认的下载处理器为：DefaultDownloadExecutor
         * 自定义下载处理器：IDownloadExecutor见文档底部接口或者源码
         * maxPoolSize：下载中的最大任务数量，默认：3（注意这里是指下载中的数量，最大发起数量不限制）
         * preferBreakpoint：是否优先使用断点下载，默认：false
         */
        .setDownloadExecutor(new DefaultDownloadExecutor(3, false))
        /**
         * 设置是否输出日志，默认：false。日志tag：IDownloadManager
         */
        .setDebug(true)
        .build(this));
```

# 接口
```
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
     * 添加回调对象，监听指定[url]的任务，如果任务不存在则回调对象不会被添加。
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
}
```

# 支持自定义底层下载处理器
```
interface IDownloadExecutor {
    /**
     * 提交下载任务
     *
     * @param request 下载请求
     * @param file    要保存的下载文件
     * @param updater 下载信息更新对象
     * @return true-提交成功；false-提交失败
     */
    fun submit(request: DownloadRequest, file: File, updater: IDownloadUpdater): Boolean

    /**
     * 取消[url]下载任务
     *
     * @return true-任务取消
     */
    fun cancel(url: String?): Boolean
}
```