package com.sd.lib.dldmgr.executor.impl;

import com.sd.lib.dldmgr.DownloadRequest;
import com.sd.lib.dldmgr.exception.DownloadHttpException;
import com.sd.lib.dldmgr.executor.DownloadExecutor;
import com.sd.lib.dldmgr.updater.DownloadUpdater;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 默认下载器
 */
public class DefaultDownloadExecutor implements DownloadExecutor
{
    private ExecutorService mExecutor;

    private ExecutorService getExecutor()
    {
        if (mExecutor == null)
        {
            synchronized (this)
            {
                if (mExecutor == null)
                {
                    mExecutor = new ThreadPoolExecutor(0, 3,
                            10L, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<Runnable>());
                }
            }
        }
        return mExecutor;
    }

    @Override
    public boolean submit(final DownloadRequest request, final File file, final DownloadUpdater updater)
    {
        final Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                final HttpRequest httpRequest = HttpRequest.get(request.getUrl());

                httpRequest.connectTimeout(15 * 1000)
                        .readTimeout(15 * 1000)
                        .trustAllCerts()
                        .trustAllCerts();

                InputStream input = null;
                OutputStream output = null;

                try
                {
                    if (httpRequest.ok())
                    {
                        final long total = httpRequest.contentLength();

                        input = httpRequest.stream();
                        output = new BufferedOutputStream(new FileOutputStream(file));

                        copy(input, output, new ProgressCallback()
                        {
                            @Override
                            public void onProgress(long count)
                            {
                                updater.notifyProgress(total, count);

                                if (total == count && count > 0)
                                    updater.notifySuccess();
                            }
                        });
                    } else
                    {
                        updater.notifyError(new DownloadHttpException(null), null);
                    }
                } catch (Exception e)
                {
                    updater.notifyError(new DownloadHttpException(e), null);
                } finally
                {
                    closeQuietly(input);
                    closeQuietly(output);
                }
            }
        };

        getExecutor().submit(runnable);
        return true;
    }


    private static void copy(InputStream in, OutputStream out, ProgressCallback callback) throws IOException
    {
        if (!(in instanceof BufferedInputStream))
            in = new BufferedInputStream(in);

        if (!(out instanceof BufferedOutputStream))
            out = new BufferedOutputStream(out);

        long count = 0;
        int len = 0;
        final byte[] buffer = new byte[1024];
        while ((len = in.read(buffer)) != -1)
        {
            out.write(buffer, 0, len);
            count += len;

            if (callback != null)
                callback.onProgress(count);
        }
        out.flush();
    }

    private static void closeQuietly(Closeable closeable)
    {
        if (closeable != null)
        {
            try
            {
                closeable.close();
            } catch (Throwable ignored)
            {
            }
        }
    }

    private interface ProgressCallback
    {
        void onProgress(long count);
    }
}
