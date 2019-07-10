package com.example.download_manager;

import com.example.download_manager.utils.HttpIOUtil;
import com.example.download_manager.utils.HttpRequest;
import com.sd.lib.dldmgr.DownloadRequest;
import com.sd.lib.dldmgr.executor.DownloadExecutor;
import com.sd.lib.dldmgr.updater.DownloadUpdater;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

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
                final HttpRequest httpRequest = HttpRequest.get(request.getUrl());
                httpRequest.trustAllCerts().trustAllCerts();

                InputStream inputStream = null;
                OutputStream outputStream = null;

                try
                {
                    if (httpRequest.ok())
                    {
                        final long total = httpRequest.contentLength();

                        inputStream = httpRequest.stream();
                        outputStream = new BufferedOutputStream(new FileOutputStream(file));

                        HttpIOUtil.copy(inputStream, outputStream, new HttpIOUtil.ProgressCallback()
                        {
                            @Override
                            public void onProgress(long count)
                            {
                                updater.notifyProgress(total, count);

                                if (total == count)
                                    updater.notifySuccess();
                            }
                        });
                    } else
                    {
                        updater.notifyError(new RuntimeException(), "");
                    }
                } catch (Exception e)
                {
                    updater.notifyError(e, "");
                } finally
                {
                    HttpIOUtil.closeQuietly(inputStream);
                    HttpIOUtil.closeQuietly(outputStream);
                }
            }
        }).start();
        return true;
    }
}
