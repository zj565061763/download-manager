package com.sd.lib.dldmgr

class DownloadRequest {
    /** 下载地址 */
    val url: String?

    /** 是否需要断点下载 */
    val preferBreakpoint: Boolean?

    private constructor(builder: Builder) {
        url = builder.url
        preferBreakpoint = builder.preferBreakpoint
    }

    class Builder {
        var url: String? = null
            private set

        var preferBreakpoint: Boolean? = null
            private set

        /**
         * 设置是否需要断点下载
         *
         * @param preferBreakpoint true-是  false-否  null-跟随默认配置
         */
        fun setPreferBreakpoint(preferBreakpoint: Boolean?): Builder {
            this.preferBreakpoint = preferBreakpoint
            return this
        }

        fun build(url: String?): DownloadRequest {
            this.url = url
            return DownloadRequest(this)
        }
    }

    companion object {
        @JvmStatic
        fun url(url: String?): DownloadRequest {
            return Builder().build(url)
        }
    }
}