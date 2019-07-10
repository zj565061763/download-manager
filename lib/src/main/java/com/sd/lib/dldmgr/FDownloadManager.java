package com.sd.lib.dldmgr;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

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
                    final DownloadManagerConfig config = DownloadManagerConfig.get();
                    sDefault = new FDownloadManager(config.getDownloadDirectory());
                }
            }
        }
        return sDefault;
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
        Log.i(TAG, "addCallback:" + callback);
    }

    @Override
    public synchronized void removeCallback(Callback callback)
    {
        if (callback == null)
            return;

        if (mListCallback.remove(callback))
            Log.i(TAG, "removeCallback:" + callback);
    }

    @Override
    public File getUrlFile(String url)
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

        return getDownloadFile(url, ext);
    }

    private synchronized File getDownloadFile(String url, String ext)
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

        final File downloadFile = getDownloadFile(url, null);
        if (downloadFile == null)
        {
            Log.e(TAG, "addTask failed create download file error:" + url);
            notifyError(info, DownloadError.CreateFile);
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
                Log.i(TAG, "addTask onSuccess:" + url);

                final File renameFile = getUrlFile(url);
                if (renameFile == null)
                {
                    Log.e(TAG, "addTask create rename file error:" + url);
                    FDownloadManager.this.notifyError(info, DownloadError.CreateFile);
                    return;
                }

                if (renameFile.exists())
                    renameFile.delete();

                if (downloadFile.renameTo(renameFile))
                {
                    FDownloadManager.this.notifySuccess(info, renameFile);
                } else
                {
                    Log.e(TAG, "addTask rename file error:" + url);
                    FDownloadManager.this.notifyError(info, DownloadError.RenameFile);
                }
            }

            @Override
            public void notifyError(Exception e, String details)
            {
                Log.e(TAG, "addTask onError:" + url + " " + e);
                FDownloadManager.this.notifyError(info, DownloadError.Http);
            }
        };

        final boolean submitted = DownloadManagerConfig.get().getDownloadExecutor().submit(downloadUpdater, downloadFile);
        if (submitted)
        {
            mMapDownloadInfo.put(url, info);
            notifyPrepare(info);
        }

        Log.i(TAG, "addTask:" + url + " path:" + downloadFile.getAbsolutePath() + " submitted:" + submitted);
        return submitted;
    }

    private void notifyPrepare(DownloadInfo downloadInfo)
    {
        downloadInfo.setState(DownloadState.Prepare);
        for (Callback item : mListCallback)
        {
            item.onPrepare(downloadInfo);
        }
    }

    private void notifyProgress(DownloadInfo downloadInfo, long total, long current)
    {
        downloadInfo.setState(DownloadState.Downloading);
        downloadInfo.getTransmitParam().transmit(total, current);
        for (Callback item : mListCallback)
        {
            item.onProgress(downloadInfo);
        }
    }

    private synchronized void notifySuccess(DownloadInfo downloadInfo, File file)
    {
        downloadInfo.setState(DownloadState.Success);
        for (Callback item : mListCallback)
        {
            item.onSuccess(downloadInfo, file);
        }

        mMapDownloadInfo.remove(downloadInfo.getUrl());
    }

    private synchronized void notifyError(DownloadInfo downloadInfo, DownloadError error)
    {
        downloadInfo.setState(DownloadState.Error);
        downloadInfo.setError(error);
        for (Callback item : mListCallback)
        {
            item.onError(downloadInfo);
        }

        mMapDownloadInfo.remove(downloadInfo.getUrl());
    }
}
