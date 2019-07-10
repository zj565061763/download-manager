package com.sd.lib.dldmgr;

public class DownloadInfo
{
    private final String mUrl;
    private DownloadState mState;
    private DownloadError mError;

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

    void setState(DownloadState state)
    {
        mState = state;
    }

    void setError(DownloadError error)
    {
        mError = error;
    }

    public TransmitParam getTransmitParam()
    {
        if (mTransmitParam == null)
            mTransmitParam = new TransmitParam();
        return mTransmitParam;
    }
}
