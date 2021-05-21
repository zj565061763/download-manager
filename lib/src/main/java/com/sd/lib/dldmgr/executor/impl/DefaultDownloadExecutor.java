package com.sd.lib.dldmgr.executor.impl;

import android.text.TextUtils;

import com.sd.lib.dldmgr.DownloadRequest;
import com.sd.lib.dldmgr.IDownloadUpdater;
import com.sd.lib.dldmgr.exception.DownloadHttpException;
import com.sd.lib.dldmgr.executor.IDownloadExecutor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 默认下载器
 */
public class DefaultDownloadExecutor implements IDownloadExecutor
{
    private ExecutorService mExecutor;
    private final Map<String, TaskInfo> mMapTask = new ConcurrentHashMap<>();

    /** 同时下载的最大任务数量 */
    private final int mMaxPoolSize;
    /** 是否需要断点下载 */
    private final boolean mPreferBreakpoint;

    public DefaultDownloadExecutor()
    {
        this(3, false);
    }

    public DefaultDownloadExecutor(int maxPoolSize, boolean preferBreakpoint)
    {
        if (maxPoolSize <= 0)
            throw new IllegalArgumentException("maxPoolSize must be > 0");
        mMaxPoolSize = maxPoolSize;
        mPreferBreakpoint = preferBreakpoint;
    }

    private ExecutorService getExecutor()
    {
        if (mExecutor != null)
            return mExecutor;

        synchronized (this)
        {
            if (mExecutor == null)
            {
                final ThreadPoolExecutor executor = new ThreadPoolExecutor(mMaxPoolSize, mMaxPoolSize,
                        10L, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>());
                executor.allowCoreThreadTimeOut(true);
                mExecutor = executor;
            }
        }
        return mExecutor;
    }

    private static HttpRequest newHttpRequest(DownloadRequest downloadRequest)
    {
        final HttpRequest httpRequest = HttpRequest.get(downloadRequest.getUrl());
        return httpRequest
                .connectTimeout(15 * 1000)
                .readTimeout(15 * 1000)
                .trustAllHosts()
                .trustAllCerts();
    }

    @Override
    public boolean submit(final DownloadRequest request, final File file, final IDownloadUpdater updater)
    {
        final Boolean requestPreferBreakpoint = request.getPreferBreakpoint();
        final boolean preferBreakpoint = requestPreferBreakpoint != null ? requestPreferBreakpoint : mPreferBreakpoint;

        final String url = request.getUrl();
        final Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                final long length = file.length();
                final boolean breakpoint = length > 0;

                HttpRequest httpRequest = newHttpRequest(request);

                if (preferBreakpoint && breakpoint)
                {
                    httpRequest.header("Range", "bytes=" + length + "-");
                }

                try
                {
                    int code = httpRequest.code();
                    if (preferBreakpoint && breakpoint)
                    {
                        if (code == HttpURLConnection.HTTP_PARTIAL)
                        {
                            downloadBreakpoint(httpRequest, file, updater);
                            return;
                        } else
                        {
                            // 不支持断点下载，尝试正常下载
                            httpRequest = newHttpRequest(request);
                            code = httpRequest.code();
                        }
                    }

                    if (code == HttpURLConnection.HTTP_OK)
                    {
                        downloadNormal(httpRequest, file, updater);
                    } else
                    {
                        updater.notifyError(new DownloadHttpException(null));
                    }
                } catch (Exception e)
                {
                    Throwable throwable = e;
                    if (e instanceof RuntimeException)
                    {
                        if (e instanceof HttpRequest.HttpRequestException)
                        {
                            final HttpRequest.HttpRequestException requestException = (HttpRequest.HttpRequestException) e;
                            final Throwable cause = requestException.getCause();
                            if (cause != null)
                                throwable = cause;
                        } else
                        {
                            throw (RuntimeException) e;
                        }
                    }

                    updater.notifyError(new DownloadHttpException(throwable));
                } finally
                {
                    mMapTask.remove(url);
                }
            }
        };

        final Future<?> future = getExecutor().submit(runnable);
        final TaskInfo taskInfo = new TaskInfo(future, updater);
        mMapTask.put(url, taskInfo);

        return true;
    }

    private void downloadNormal(HttpRequest request, File file, final IDownloadUpdater updater) throws IOException
    {
        InputStream input = null;
        OutputStream output = null;

        try
        {
            input = request.stream();
            output = new BufferedOutputStream(new FileOutputStream(file));

            final long total = request.contentLength();
            final OutputStream finalOut = output;
            read(input, new ReadCallback()
            {
                @Override
                public void write(byte[] buffer, int offset, int length) throws IOException
                {
                    finalOut.write(buffer, offset, length);
                }

                @Override
                public void count(int count)
                {
                    updater.notifyProgress(total, count);
                }
            });
            output.flush();
            updater.notifySuccess();
        } finally
        {
            closeQuietly(input);
            closeQuietly(output);
        }
    }

    private void downloadBreakpoint(HttpRequest request, File file, final IDownloadUpdater updater) throws IOException
    {
        final long length = file.length();
        if (length <= 0)
            throw new RuntimeException("file length must > 0");

        InputStream input = null;
        RandomAccessFile randomAccessFile = null;

        try
        {
            input = request.stream();
            randomAccessFile = new RandomAccessFile(file, "rwd");
            randomAccessFile.seek(length);

            final long total = request.contentLength() + length;
            final RandomAccessFile finalFile = randomAccessFile;
            read(input, new ReadCallback()
            {
                @Override
                public void write(byte[] buffer, int offset, int length) throws IOException
                {
                    finalFile.write(buffer, offset, length);
                }

                @Override
                public void count(int count)
                {
                    updater.notifyProgress(total, count + length);
                }
            });
            updater.notifySuccess();
        } finally
        {
            closeQuietly(input);
            closeQuietly(randomAccessFile);
        }
    }

    @Override
    public boolean cancel(String url)
    {
        if (TextUtils.isEmpty(url))
            return false;

        final TaskInfo taskInfo = mMapTask.remove(url);
        if (taskInfo == null)
            return false;

        if (taskInfo.mFuture.isDone())
            return false;

        taskInfo.mUpdater.notifyCancel();
        taskInfo.mFuture.cancel(true);
        return true;
    }

    private static final class TaskInfo
    {
        private final Future<?> mFuture;
        private final IDownloadUpdater mUpdater;

        public TaskInfo(Future<?> future, IDownloadUpdater updater)
        {
            if (future == null || updater == null)
                throw new IllegalArgumentException();

            mFuture = future;
            mUpdater = updater;
        }
    }

    private static void read(InputStream in, ReadCallback callback) throws IOException
    {
        if (!(in instanceof BufferedInputStream))
            in = new BufferedInputStream(in);

        int count = 0;
        int length = 0;
        final byte[] buffer = new byte[10 * 1024];
        while ((length = in.read(buffer)) != -1)
        {
            callback.write(buffer, 0, length);
            count += length;
            callback.count(count);
        }
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

    private interface ReadCallback
    {
        void write(byte[] buffer, int offset, int length) throws IOException;

        void count(int count);
    }
}
