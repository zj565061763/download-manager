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