package com.sd.lib.dldmgr;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.sd.lib.dldmgr.exception.DownloadHttpException;
import com.sd.lib.dldmgr.updater.DownloadUpdater;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FDownloadManager implements DownloadManager
{
    private static FDownloadManager sDefault = null;

    private final File mDirectory;
    private final Map<String, DownloadInfo> mMapDownloadInfo = new ConcurrentHashMap<>();
    private final List<Callback> mListCallback = new CopyOnWriteArrayList<>();

    protected FDownloadManager(String directory)
    {
        if (directory == null)
            throw new IllegalArgumentException("directory is null");

        mDirectory = new File(directory);
    }

    public static FDownloadManager getDefault()
    {
        if (sDefault == null)
        {
            synchronized (FDownloadManager.class)
            {
                if (sDefault == null)
                {
                    sDefault = new FDownloadManager(getConfig().getDownloadDirectory());
                }
            }
        }
        return sDefault;
    }

    private static DownloadManagerConfig getConfig()
    {
        return DownloadManagerConfig.get();
    }

    private boolean checkDirectory()
    {
        return Utils.mkdirs(mDirectory);
    }

    @Override
    public synchronized void addCallback(Callback callback)
    {
        if (callback == null)
            return;

        if (mListCallback.contains(callback))
            return;

        mListCallback.add(callback);

        if (getConfig().isDebug())
            Log.i(TAG, "addCallback:" + callback);
    }

    @Override
    public synchronized void removeCallback(Callback callback)
    {
        if (callback == null)
            return;

        if (mListCallback.remove(callback))
        {
            if (getConfig().isDebug())
                Log.i(TAG, "removeCallback:" + callback);
        }
    }

    @Override
    public File getDownloadFile(String url)
    {
        if (TextUtils.isEmpty(url))
            return null;

        String ext = null;
        try
        {
            final Uri uri = Uri.parse(url);
            final String path = uri.getPath();
            if (!TextUtils.isEmpty(path))
            {
                ext = path.substring(path.lastIndexOf("."));
            } else
            {
                ext = url.substring(url.lastIndexOf("."));
            }
        } catch (Exception e)
        {
        }

        return getUrlFile(url, ext);
    }

    private synchronized File getUrlFile(String url, String ext)
    {
        if (TextUtils.isEmpty(url))
            throw new IllegalArgumentException("url is empty");

        if (!checkDirectory())
            return null;

        if (ext == null)
            ext = "";

        final String fileName = Utils.MD5(url) + ext;
        return new File(mDirectory, fileName);
    }

    @Override
    public DownloadInfo getDownloadInfo(String url)
    {
        return mMapDownloadInfo.get(url);
    }

    @Override
    public synchronized boolean addTask(final String url)
    {
        if (TextUtils.isEmpty(url))
            return false;

        if (mMapDownloadInfo.containsKey(url))
            return false;

        final DownloadInfo info = new DownloadInfo(url);

        final File tempFile = getUrlFile(url, ".temp");
        if (tempFile == null)
        {
            if (getConfig().isDebug())
                Log.e(TAG, "addTask error create temp file error:" + url);

            notifyError(info, DownloadError.CreateTempFile);
            return false;
        }

        final DownloadUpdater downloadUpdater = new DownloadUpdater()
        {
            @Override
            public void notifyProgress(long total, long current)
            {
                FDownloadManager.this.notifyProgress(info, total, current);
            }

            @Override
            public void notifySuccess()
            {
                if (getConfig().isDebug())
                    Log.i(TAG, "download success:" + url);

                if (!tempFile.exists())
                {
                    if (getConfig().isDebug())
                        Log.e(TAG, "download success error temp file not exists:" + url);

                    FDownloadManager.this.notifyError(info, DownloadError.TempFileNotExists);
                    return;
                }

                final File downloadFile = getDownloadFile(url);
                if (downloadFile == null)
                {
                    if (getConfig().isDebug())
                        Log.e(TAG, "download success error create download file:" + url);

                    FDownloadManager.this.notifyError(info, DownloadError.CreateDownloadFile);
                    return;
                }

                if (downloadFile.exists())
                    downloadFile.delete();

                if (tempFile.renameTo(downloadFile))
                {
                    FDownloadManager.this.notifySuccess(info, downloadFile);
                } else
                {
                    if (getConfig().isDebug())
                        Log.e(TAG, "download success error rename temp file to download file:" + url);

                    FDownloadManager.this.notifyError(info, DownloadError.RenameFile);
                }
            }

            @Override
            public void notifyError(Exception e, String details)
            {
                if (getConfig().isDebug())
                    Log.e(TAG, "download error:" + url + " " + e);

                final DownloadError error = e instanceof DownloadHttpException ? DownloadError.Http : DownloadError.Other;
                FDownloadManager.this.notifyError(info, error);
            }
        };

        final DownloadRequest downloadRequest = new DownloadRequest(url);
        final boolean submitted = getConfig().getDownloadExecutor().submit(downloadRequest, tempFile, downloadUpdater);
        if (submitted)
        {
            mMapDownloadInfo.put(url, info);
            notifyPrepare(info);

            if (getConfig().isDebug())
                Log.i(TAG, "addTask:" + url + " path:" + tempFile.getAbsolutePath() + " size:" + mMapDownloadInfo.size());
        } else
        {
            if (getConfig().isDebug())
                Log.e(TAG, "addTask error submit request failed:" + url);

            FDownloadManager.this.notifyError(info, DownloadError.SubmitFailed);
        }

        return submitted;
    }

    private void notifyPrepare(DownloadInfo info)
    {
        info.setState(DownloadState.Prepare);
        mMainThreadCallback.onPrepare(info);
    }

    private void notifyProgress(DownloadInfo info, long total, long current)
    {
        info.setState(DownloadState.Downloading);
        final boolean changed = info.getTransmitParam().transmit(total, current);

        if (changed)
            mMainThreadCallback.onProgress(info);
    }

    private void notifySuccess(DownloadInfo info, File file)
    {
        info.setState(DownloadState.Success);
        mMainThreadCallback.onSuccess(info, file);

        removeDownloadInfo(info);
    }

    private void notifyError(DownloadInfo info, DownloadError error)
    {
        info.setState(DownloadState.Error);
        info.setError(error);
        mMainThreadCallback.onError(info);

        removeDownloadInfo(info);
    }

    private synchronized void removeDownloadInfo(DownloadInfo info)
    {
        mMapDownloadInfo.remove(info.getUrl());
    }

    private final Callback mMainThreadCallback = new Callback()
    {
        @Override
        public void onPrepare(final DownloadInfo info)
        {
            Utils.runOnMainThread(new Runnable()
            {
                @Override
                public void run()
                {
                    for (Callback item : mListCallback)
                    {
                        item.onPrepare(info);
                    }
                }
            });
        }

        @Override
        public void onProgress(final DownloadInfo info)
        {
            Utils.runOnMainThread(new Runnable()
            {
                @Override
                public void run()
                {
                    for (Callback item : mListCallback)
                    {
                        item.onProgress(info);
                    }
                }
            });
        }

        @Override
        public void onSuccess(final DownloadInfo info, final File file)
        {
            Utils.runOnMainThread(new Runnable()
            {
                @Override
                public void run()
                {
                    for (Callback item : mListCallback)
                    {
                        item.onSuccess(info, file);
                    }
                }
            });
        }

        @Override
        public void onError(final DownloadInfo info)
        {
            Utils.runOnMainThread(new Runnable()
            {
                @Override
                public void run()
                {
                    for (Callback item : mListCallback)
                    {
                        item.onError(info);
                    }
                }
            });
        }
    };
}
