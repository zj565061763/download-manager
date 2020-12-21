package com.sd.lib.dldmgr;

import java.io.File;

public interface IDownloadDirectory
{
    boolean copyFile(File file);

    int deleteFile(String ext);

    int deleteTempFile(FileInterceptor interceptor);

    interface FileCallback
    {
        void onFile(File file);
    }

    interface FileInterceptor
    {
        boolean intercept(File file);
    }
}
