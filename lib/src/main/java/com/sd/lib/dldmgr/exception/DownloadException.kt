package com.sd.lib.dldmgr.exception;

public class DownloadException extends Exception
{
    public DownloadException(Throwable cause)
    {
        super(cause);
    }

    public DownloadException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
