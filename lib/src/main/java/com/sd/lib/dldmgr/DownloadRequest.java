package com.sd.lib.dldmgr;

import android.text.TextUtils;

public class DownloadRequest
{
    private final String mUrl;

    public DownloadRequest(String url)
    {
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
}
