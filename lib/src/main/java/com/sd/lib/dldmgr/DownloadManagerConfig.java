package com.sd.lib.dldmgr;

import android.content.Context;
import android.text.TextUtils;

import com.sd.lib.dldmgr.executor.impl.DefaultDownloadExecutor;
import com.sd.lib.dldmgr.executor.DownloadExecutor;

import java.io.File;

/**
 * 下载器配置
 */
public class DownloadManagerConfig
{
    private static DownloadManagerConfig sConfig;

    private final boolean mIsDebug;

    private final Context mContext;
    private final DownloadExecutor mDownloadExecutor;
    private final String mDownloadDirectory;

    private DownloadManagerConfig(Builder builder)
    {
        mIsDebug = builder.mIsDebug;
        mContext = builder.mContext;
        mDownloadExecutor = builder.mDownloadExecutor != null ? builder.mDownloadExecutor : new DefaultDownloadExecutor();

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

    public boolean isDebug()
    {
        return mIsDebug;
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
        private boolean mIsDebug = false;

        private Context mContext;
        private DownloadExecutor mDownloadExecutor;
        private String mDownloadDirectory;

        /**
         * 设置调试模式
         *
         * @param debug
         * @return
         */
        public Builder setDebug(boolean debug)
        {
            mIsDebug = debug;
            return this;
        }

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
         * @return
         */
        public Builder setDownloadDirectory(String directory)
        {
            mDownloadDirectory = directory;
            return this;
        }

        /**
         * 构建对象
         *
         * @param context
         * @return
         */
        public DownloadManagerConfig build(Context context)
        {
            mContext = context.getApplicationContext();
            return new DownloadManagerConfig(this);
        }
    }
}
