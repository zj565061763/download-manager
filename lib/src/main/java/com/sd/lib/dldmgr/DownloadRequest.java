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

    public String getUrl()
    {
        return mUrl;
    }
}
