package com.sd.lib.dldmgr

import android.content.Context
import com.sd.lib.dldmgr.executor.IDownloadExecutor
import com.sd.lib.dldmgr.executor.impl.DefaultDownloadExecutor

/**
 * 下载器配置
 */
class DownloadManagerConfig {
    val isDebug: Boolean
    val context: Context
    val downloadDirectory: String
    val downloadExecutor: IDownloadExecutor

    private constructor(builder: Builder) {
        isDebug = builder.isDebug
        context = builder.context!!

        var dir = builder.downloadDirectory
        if (dir == null || dir.isEmpty()) {
            val dirFile = Utils.getCacheDir("fdownload", context)
            dir = dirFile.absolutePath
        }
        downloadDirectory = dir!!
        downloadExecutor = builder.downloadExecutor ?: DefaultDownloadExecutor()
    }

    class Builder {
        var isDebug = false
            private set

        var context: Context? = null
            private set

        var downloadDirectory: String? = null
            private set

        var downloadExecutor: IDownloadExecutor? = null
            private set

        /**
         * 设置调试模式
         */
        fun setDebug(debug: Boolean): Builder {
            isDebug = debug
            return this
        }

        /**
         * 设置下载目录
         */
        fun setDownloadDirectory(directory: String?): Builder {
            downloadDirectory = directory
            return this
        }

        /**
         * 设置下载执行器
         */
        fun setDownloadExecutor(executor: IDownloadExecutor?): Builder {
            downloadExecutor = executor
            return this
        }

        fun build(context: Context): DownloadManagerConfig {
            this.context = context.applicationContext
            return DownloadManagerConfig(this)
        }
    }

    companion object {
        @JvmStatic
        private var _config: DownloadManagerConfig? = null

        /**
         * 返回配置
         */
        @JvmStatic
        fun get(): DownloadManagerConfig {
            return _config ?: throw RuntimeException("${DownloadManagerConfig::class.java.simpleName} has not been initialized")
        }

        /**
         * 初始化
         *
         * @param config
         */
        @JvmStatic
        @Synchronized
        fun init(config: DownloadManagerConfig) {
            if (_config != null) {
                throw RuntimeException("${DownloadManagerConfig::class.java.simpleName} has been initialized")
            }
            _config = config
        }
    }
}