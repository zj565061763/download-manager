package com.sd.lib.dldmgr;

import android.text.TextUtils;

public class DownloadRequest
{
    private final String mUrl;
    private final Boolean mPreferBreakpoint;

    private DownloadRequest(Builder builder)
    {
        final String url = builder.url;
        if (TextUtils.isEmpty(url))
            throw new IllegalArgumentException("url is empty");

        mUrl = url;
        mPreferBreakpoint = builder.preferBreakpoint;
    }

    /**
     * 下载地址
     *
     * @return
     */
    public String getUrl()
    {
        return mUrl;
    }

    /**
     * 是否需要断点下载
     *
     * @return
     */
    public Boolean getPreferBreakpoint()
    {
        return mPreferBreakpoint;
    }

    public static class Builder
    {
        private String url;
        private Boolean preferBreakpoint;

        /**
         * 设置是否需要断点下载
         *
         * @param preferBreakpoint true-需要；false-不需要；null-跟随默认配置
         * @return
         */
        public Builder setPreferBreakpoint(Boolean preferBreakpoint)
        {
            this.preferBreakpoint = preferBreakpoint;
            return this;
        }

        public DownloadRequest build(String url)
        {
            this.url = url;
            return new DownloadRequest(this);
        }
    }
}
