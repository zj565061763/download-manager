package com.sd.lib.dldmgr;

public class DownloadInfo
{
    private final String mUrl;

    private volatile DownloadState mState;
    private DownloadError mError;
    private Throwable mThrowable;

    private TransmitParam mTransmitParam = new TransmitParam();

    DownloadInfo(String url)
    {
        if (url == null)
            throw new NullPointerException("url is null");

        mUrl = url;
    }

    public DownloadInfo copy()
    {
        final DownloadInfo info = new DownloadInfo(mUrl);
        info.mState = mState;
        info.mError = mError;
        info.mThrowable = mThrowable;
        info.mTransmitParam = mTransmitParam.copy();
        return info;
    }

    public String getUrl()
    {
        return mUrl;
    }

    public DownloadState getState()
    {
        return mState;
    }

    public DownloadError getError()
    {
        return mError;
    }

    public Throwable getThrowable()
    {
        return mThrowable;
    }

    public TransmitParam getTransmitParam()
    {
        return mTransmitParam;
    }

    void setState(DownloadState state)
    {
        mState = state;
    }

    void setError(DownloadError error)
    {
        mError = error;
    }

    void setThrowable(Throwable throwable)
    {
        mThrowable = throwable;
    }
}
