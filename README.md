# About
安卓下载管理

# Gradle
[![](https://jitpack.io/v/zj565061763/download-manager.svg)](https://jitpack.io/#zj565061763/download-manager)

# Demo
```java
public class MainActivity extends AppCompatActivity
{
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String URL = "https://dldir1.qq.com/qqfile/qq/PCQQ9.1.5/25530/QQ9.1.5.25530.exe";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_download).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // 添加一个下载任务
                FDownloadManager.getDefault().addTask(URL);
            }
        });

        // 添加下载回调
        FDownloadManager.getDefault().addCallback(mDownloadCallback);
    }

    private final DownloadManager.Callback mDownloadCallback = new DownloadManager.Callback()
    {
        @Override
        public void onPrepare(DownloadInfo info)
        {
            Log.i(TAG, "onPrepare:" + info.getUrl());
        }

        @Override
        public void onProgress(DownloadInfo info)
        {
            final TransmitParam param = info.getTransmitParam();

            // 下载进度
            final int progress = param.getProgress();
            // 下载速率
            final int speed = param.getSpeedKBps();

            Log.i(TAG, "onProgress:" + progress + " " + speed);
        }

        @Override
        public void onSuccess(DownloadInfo info, File file)
        {
            Log.i(TAG, "onSuccess:" + info.getUrl() + "\r\n"
                    + " file:" + file.getAbsolutePath());
        }

        @Override
        public void onError(DownloadInfo info)
        {
            Log.e(TAG, "onError:" + info.getError());
        }
    };

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        // 移除下载回调
        FDownloadManager.getDefault().removeCallback(mDownloadCallback);
    }
}
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