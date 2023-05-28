package com.sd.lib.dldmgr

import android.content.Context
import com.sd.lib.dldmgr.executor.IDownloadExecutor
import com.sd.lib.dldmgr.executor.impl.DefaultDownloadExecutor
import java.io.File

/**
 * 下载器配置
 */
class DownloadManagerConfig private constructor(builder: Builder) {
    internal val isDebug: Boolean
    internal val downloadDirectory: File
    internal val downloadExecutor: IDownloadExecutor

    init {
        isDebug = builder.isDebug
        downloadDirectory = builder.downloadDirectory ?: Utils.getCacheDir("fdownload", builder.context)
        downloadExecutor = builder.downloadExecutor ?: DefaultDownloadExecutor()
    }

    class Builder {
        internal lateinit var context: Context
            private set

        internal var isDebug = false
            private set

        internal var downloadDirectory: File? = null
            private set

        internal var downloadExecutor: IDownloadExecutor? = null
            private set

        /**
         * 调试模式
         */
        fun setDebug(debug: Boolean) = apply {
            this.isDebug = debug
        }

        /**
         * 下载目录
         */
        fun setDownloadDirectory(directory: File?) = apply {
            this.downloadDirectory = directory
        }

        /**
         * 下载执行器
         */
        fun setDownloadExecutor(executor: IDownloadExecutor?) = apply {
            this.downloadExecutor = executor
        }

        fun build(context: Context): DownloadManagerConfig {
            this.context = context.applicationContext
            return DownloadManagerConfig(this)
        }
    }

    companion object {
        @Volatile
        private var sConfig: DownloadManagerConfig? = null

        /**
         * 初始化
         */
        @JvmStatic
        fun init(config: DownloadManagerConfig) {
            synchronized(this) {
                if (sConfig == null) {
                    sConfig = config
                }
            }
        }

        /**
         * 返回配置
         */
        @JvmStatic
        fun get(): DownloadManagerConfig {
            val config = sConfig
            if (config != null) return config
            synchronized(this) {
                return sConfig ?: error("DownloadManagerConfig has not been initialized")
            }
        }
    }
}