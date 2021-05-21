package com.example.download_manager

import android.app.Application
import com.sd.lib.dldmgr.DownloadManagerConfig
import com.sd.lib.dldmgr.DownloadManagerConfig.Companion.init

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        init(
            DownloadManagerConfig.Builder()
                .setDebug(true)
                .build(this)
        )
    }
}