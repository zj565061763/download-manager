package com.sd.lib.dldmgr;

import android.content.Context;
import android.text.TextUtils;

import com.sd.lib.dldmgr.executor.DownloadExecutor;

import java.io.File;

/**
 * 下载器配置
 */
public class DownloadManagerConfig
{
    private static DownloadManagerConfig sConfig;

    private final Context mContext;
    private final DownloadExecutor mDownloadExecutor;
    private final String mDownloadDirectory;

    private DownloadManagerConfig(Builder builder)
    {
        mContext = builder.mContext;
        mDownloadExecutor = builder.mDownloadExecutor;

        String dir = builder.mDownloadDirectory;
        if (TextUtils.isEmpty(dir))
        {
            final File dirFile = Utils.getCacheDir("download", mContext);
            dir = dirFile.getAbsolutePath();
        }
        mDownloadDirectory = dir;
    }

    /**
     * 返回配置
     *
     * @return
     */
    public static DownloadManagerConfig get()
    {
        if (sConfig == null)
            throw new RuntimeException(DownloadManagerConfig.class.getSimpleName() + "has not been init");
        return sConfig;
    }

    /**
     * 初始化
     *
     * @param config
     */
    public static synchronized void init(DownloadManagerConfig config)
    {
        if (config == null)
            throw new IllegalArgumentException("config is null");

        if (sConfig != null)
            throw new RuntimeException(DownloadManagerConfig.class.getSimpleName() + " has been init");

        config.checkConfig();
        sConfig = config;
    }

    private void checkConfig()
    {
        if (mDownloadExecutor == null)
            throw new RuntimeException(DownloadExecutor.class.getSimpleName() + " is null");
    }

    public Context getContext()
    {
        return mContext;
    }

    public DownloadExecutor getDownloadExecutor()
    {
        return mDownloadExecutor;
    }

    public String getDownloadDirectory()
    {
        return mDownloadDirectory;
    }

    public static class Builder
    {
        private Context mContext;
        private DownloadExecutor mDownloadExecutor;
        private String mDownloadDirectory;

        /**
         * 设置下载执行器
         *
         * @param executor
         * @return
         */
        public Builder setDownloadExecutor(DownloadExecutor executor)
        {
            mDownloadExecutor = executor;
            return this;
        }

        /**
         * 设置下载目录
         *
         * @param directory
         */
        public void setDownloadDirectory(String directory)
        {
            mDownloadDirectory = directory;
        }

        public DownloadManagerConfig build(Context context)
        {
            mContext = context.getApplicationContext();
            return new DownloadManagerConfig(this);
        }
    }
}
