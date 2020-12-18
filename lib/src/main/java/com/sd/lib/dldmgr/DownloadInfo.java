package com.sd.lib.dldmgr;

public class DownloadInfo
{
    private final String mUrl;
    private DownloadState mState;
    private DownloadError mError;
    private Throwable mThrowable;

    private TransmitParam mTransmitParam;

    public DownloadInfo(String url)
    {
        mUrl = url;
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

    public TransmitParam getTransmitParam()
    {
        if (mTransmitParam == null)
            mTransmitParam = new TransmitParam();
        return mTransmitParam;
    }
}
