package com.example.download_manager;

import com.example.download_manager.utils.HttpRequest;
import com.sd.lib.dldmgr.DownloadRequest;
import com.sd.lib.dldmgr.executor.DownloadExecutor;
import com.sd.lib.dldmgr.updater.DownloadUpdater;

import java.io.File;

public class SimpleDownloadExecutor implements DownloadExecutor
{
    @Override
    public boolean submit(final DownloadRequest request, final File file, final DownloadUpdater updater)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    HttpRequest.get(request.getUrl()).progress(new HttpRequest.UploadProgress()
                    {
                        @Override
                        public void onUpload(long uploaded, long total)
                        {
                            updater.notifyProgress(total, uploaded);

                            if (total > 0 && uploaded == total)
                            {
                                updater.notifySuccess();
                            }
                        }
                    }).receive(file);
                } catch (Exception e)
                {
                    updater.notifyError(e, "");
                }
            }
        }).start();
        return true;
    }
}
