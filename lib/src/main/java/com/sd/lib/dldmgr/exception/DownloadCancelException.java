package com.sd.lib.dldmgr.exception;

public class DownloadCancelException extends DownloadException
{
    public DownloadCancelException(Throwable cause)
    {
        super(cause);
    }

    public DownloadCancelException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
