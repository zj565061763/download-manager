package com.sd.lib.dldmgr;

import android.text.TextUtils;

public class DownloadRequest
{
    private final String mUrl;

    private DownloadRequest(Builder builder)
    {
        final String url = builder.url;
        if (TextUtils.isEmpty(url))
            throw new IllegalArgumentException("url is empty");

        mUrl = url;
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

    public static class Builder
    {
        private String url;

        public DownloadRequest build(String url)
        {
            this.url = url;
            return new DownloadRequest(this);
        }
    }
}
