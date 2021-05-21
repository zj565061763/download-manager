package com.sd.lib.dldmgr.exception;

public class DownloadHttpException extends DownloadException
{
    public DownloadHttpException(Throwable cause)
    {
        super(cause);
    }

    public DownloadHttpException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
