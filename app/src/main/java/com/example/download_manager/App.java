package com.example.download_manager;

import android.app.Application;

import com.sd.lib.dldmgr.DownloadManagerConfig;

public class App extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

        DownloadManagerConfig.init(new DownloadManagerConfig.Builder()
                .setDebug(true)
                .setDownloadExecutor(new SimpleDownloadExecutor())
                .build(this));
    }
}
