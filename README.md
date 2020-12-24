# About
安卓下载管理

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

# 下载

* 简单下载
```java
// 添加下载任务
final boolean addTask = FDownloadManager.getDefault().addTask(url);
```

* 断点下载
```java
// 创建下载请求对象
final DownloadRequest downloadRequest = new DownloadRequest.Builder()
        // 设置断点下载，true-优先断点下载；false-不使用断点下载；null-跟随初始化配置
        .setPreferBreakpoint(true)
        // 下载地址
        .build(url);
// 添加下载任务
final boolean addTask = FDownloadManager.getDefault().addTask(downloadRequest);
```

* 下载监听
```java

/**
 * 添加下载回调
 * 由于下载回调是全局的监听，所以一些特殊业务需要在回调方法里面判断下载url和业务url是否一样
 */
FDownloadManager.getDefault().addCallback(mDownloadCallback);
// 移除下载回调
FDownloadManager.getDefault().removeCallback(mDownloadCallback);

/**
 * 下载回调
 */
private final IDownloadManager.Callback mDownloadCallback = new IDownloadManager.Callback()
{
    @Override
    public void onPrepare(DownloadInfo info)
    {
        // 准备下载（已提交未开始）
    }

    @Override
    public void onProgress(DownloadInfo info)
    {
        // 下载信息
        final TransmitParam param = info.getTransmitParam();
        // 下载进度
        final int progress = param.getProgress();
        // 下载速率
        final int speed = param.getSpeedKBps();
    }

    @Override
    public void onSuccess(DownloadInfo info, File file)
    {
        // 下载成功
    }

    @Override
    public void onError(DownloadInfo info)
    {
        // 下载失败
        final DownloadError error = info.getError();
    }
};
```

# 文件处理器
有时候需要在下载成功之后，把文件拷贝或者移动到其他目录，这时候需要用到文件处理器

* CopyFileProcessor 拷贝文件处理器
* TakeFileProcessor 移动文件处理器

#### 处理器接口
```java
/**
 * 文件处理器
 */
interface FileProcessor
{
    void process(File file);
}
```

#### 添加处理器
```java
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
```

```java
// 获取要保存的目录
final File directory = getExternalFilesDir("my_download");
// 创建下载目录管理对象
final IDownloadDirectory downloadDirectory = DownloadDirectory.from(directory);
// 创建文件拷贝处理器
final IDownloadManager.FileProcessor copyFileProcessor = new CopyFileProcessor(downloadDirectory);
// 添加文件处理器
final boolean addFileProcessor = FDownloadManager.getDefault().addFileProcessor(mUrl, copyFileProcessor);
```

# 接口
```java
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
     * 删除所有临时文件（下载中的临时文件不会被删除）
     */
    void deleteTempFile();

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
     * 返回下载信息
     *
     * @param url
     * @return
     */
    DownloadInfo getDownloadInfo(String url);

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
        /**
         * 准备下载（已提交未开始）
         *
         * @param info
         */
        void onPrepare(DownloadInfo info);

        /**
         * 下载中
         *
         * @param info
         */
        void onProgress(DownloadInfo info);

        /**
         * 下载成功
         *
         * @param info
         * @param file 下载文件
         */
        void onSuccess(DownloadInfo info, File file);

        /**
         * 下载失败
         *
         * @param info
         */
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
```