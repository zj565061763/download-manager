package com.example.download_manager

import android.app.Application
import com.sd.lib.dldmgr.DownloadManagerConfig

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DownloadManagerConfig.init(
            DownloadManagerConfig.Builder()
                .setDebug(true)
                .build(this)
        )
    }
}