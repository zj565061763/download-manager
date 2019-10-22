package com.sd.lib.dldmgr;

import android.text.TextUtils;
import android.util.Log;

import com.sd.lib.dldmgr.exception.DownloadCancelException;
import com.sd.lib.dldmgr.exception.DownloadHttpException;
import com.sd.lib.dldmgr.updater.DownloadUpdater;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FDownloadManager implements DownloadManager
{
    private static final String EXT_TEMP = ".temp";
    private static FDownloadManager sDefault = null;

    private final File mDirectory;
    private final Map<String, DownloadInfoWrapper> mMapDownloadInfo = new ConcurrentHashMap<>();
    private final Map<File, String> mMapTempFile = new ConcurrentHashMap<>();

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
            Log.i(TAG, "addCallback:" + callback + " size:" + mListCallback.size());
    }

    @Override
    public synchronized void removeCallback(Callback callback)
    {
        if (callback == null)
            return;

        if (mListCallback.remove(callback))
        {
            if (getConfig().isDebug())
                Log.i(TAG, "removeCallback:" + callback + " size:" + mListCallback.size());
        }
    }

    @Override
    public File getDownloadFile(String url)
    {
        if (TextUtils.isEmpty(url))
            return null;

        final File file = getDownloadFileInternal(url);
        if (file == null)
            return null;

        return file.exists() ? file : null;
    }

    private File getTempFile(String url)
    {
        return getUrlFile(url, EXT_TEMP);
    }

    private File getDownloadFileInternal(String url)
    {
        return getUrlFile(url, Utils.getExt(url));
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
        final DownloadInfoWrapper wrapper = mMapDownloadInfo.get(url);
        if (wrapper == null)
            return null;

        return wrapper.mDownloadInfo;
    }

    private File[] getAllFile()
    {
        if (!checkDirectory())
            return null;

        final File[] files = mDirectory.listFiles();
        if (files == null || files.length <= 0)
            return null;

        return files;
    }

    @Override
    public synchronized void deleteTempFile()
    {
        final File[] files = getAllFile();
        if (files == null || files.length <= 0)
            return;

        try
        {
            int count = 0;
            for (File item : files)
            {
                if (mMapTempFile.containsKey(item))
                    continue;

                final String name = item.getName();
                if (name.endsWith(EXT_TEMP))
                {
                    if (item.delete())
                        count++;
                }
            }

            if (getConfig().isDebug())
                Log.i(TAG, "deleteTempFile count:" + count);

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void deleteDownloadFile(String ext)
    {
        final File[] files = getAllFile();
        if (files == null || files.length <= 0)
            return;

        try
        {
            int count = 0;
            for (File item : files)
            {
                final String name = item.getName();
                if (name.endsWith(EXT_TEMP))
                    continue;

                final boolean delete = TextUtils.isEmpty(ext) || name.endsWith(ext);
                if (delete)
                {
                    if (item.delete())
                        count++;
                }
            }

            if (getConfig().isDebug())
                Log.i(TAG, "deleteDownloadFile count:" + count + " ext:" + ext);

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized boolean addTask(final String url)
    {
        if (TextUtils.isEmpty(url))
            return false;

        if (mMapDownloadInfo.containsKey(url))
            return false;

        final DownloadInfo info = new DownloadInfo(url);

        final File tempFile = getTempFile(url);
        if (tempFile == null)
        {
            if (getConfig().isDebug())
                Log.e(TAG, "addTask error create temp file error:" + url);

            notifyError(info, DownloadError.CreateTempFile);
            return false;
        }

        final DownloadRequest downloadRequest = new DownloadRequest(url);
        final DownloadUpdater downloadUpdater = new InternalDownloadUpdater(info, tempFile);

        final boolean submitted = getConfig().getDownloadExecutor().submit(downloadRequest, tempFile, downloadUpdater);
        if (submitted)
        {
            final DownloadInfoWrapper wrapper = new DownloadInfoWrapper(info, tempFile);
            mMapDownloadInfo.put(url, wrapper);
            mMapTempFile.put(tempFile, url);

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

    @Override
    public synchronized boolean cancelTask(String url)
    {
        if (TextUtils.isEmpty(url))
            return false;

        final DownloadInfoWrapper wrapper = removeDownloadInfo(url);
        if (wrapper == null)
            return false;

        if (getConfig().isDebug())
            Log.i(TAG, "cancelTask:" + url);

        return getConfig().getDownloadExecutor().cancel(url);
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

        removeDownloadInfo(info.getUrl());
    }

    private void notifyError(DownloadInfo info, DownloadError error)
    {
        info.setState(DownloadState.Error);
        info.setError(error);
        mMainThreadCallback.onError(info);

        removeDownloadInfo(info.getUrl());
    }

    /**
     * 任务结束，移除下载信息
     *
     * @param url
     * @return
     */
    private synchronized DownloadInfoWrapper removeDownloadInfo(String url)
    {
        final DownloadInfoWrapper wrapper = mMapDownloadInfo.remove(url);
        if (wrapper != null)
        {
            mMapTempFile.remove(wrapper.mTempFile);

            if (getConfig().isDebug())
                Log.i(TAG, "removeDownloadInfo:" + url + " size:" + mMapDownloadInfo.size() + " tempSize:" + mMapTempFile.size());
        }
        return wrapper;
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

    private final class InternalDownloadUpdater implements DownloadUpdater
    {
        private final DownloadInfo mInfo;
        private final File mTempFile;

        private final String mUrl;
        private volatile boolean mCompleted = false;

        public InternalDownloadUpdater(DownloadInfo info, File tempFile)
        {
            if (info == null)
                throw new IllegalArgumentException("info is null for updater");

            if (tempFile == null)
                throw new IllegalArgumentException("tempFile is null for updater");

            mInfo = info;
            mTempFile = tempFile;
            mUrl = info.getUrl();
        }

        @Override
        public void notifyProgress(long total, long current)
        {
            if (mCompleted)
                return;

            FDownloadManager.this.notifyProgress(mInfo, total, current);
        }

        @Override
        public void notifySuccess()
        {
            if (mCompleted)
                return;

            mCompleted = true;

            if (getConfig().isDebug())
                Log.i(TAG, "download success:" + mUrl);

            if (!mTempFile.exists())
            {
                if (getConfig().isDebug())
                    Log.e(TAG, "download success error temp file not exists:" + mUrl);

                FDownloadManager.this.notifyError(mInfo, DownloadError.TempFileNotExists);
                return;
            }

            final File downloadFile = getDownloadFileInternal(mUrl);
            if (downloadFile == null)
            {
                if (getConfig().isDebug())
                    Log.e(TAG, "download success error create download file:" + mUrl);

                FDownloadManager.this.notifyError(mInfo, DownloadError.CreateDownloadFile);
                return;
            }

            if (downloadFile.exists())
                downloadFile.delete();

            if (mTempFile.renameTo(downloadFile))
            {
                FDownloadManager.this.notifySuccess(mInfo, downloadFile);
            } else
            {
                if (getConfig().isDebug())
                    Log.e(TAG, "download success error rename temp file to download file:" + mUrl);

                FDownloadManager.this.notifyError(mInfo, DownloadError.RenameFile);
            }
        }

        @Override
        public void notifyError(Exception e, String details)
        {
            if (mCompleted)
                return;

            mCompleted = true;

            if (getConfig().isDebug())
                Log.e(TAG, "download error:" + mUrl + " " + e);

            DownloadError error = DownloadError.Other;
            if (e instanceof DownloadHttpException)
            {
                error = DownloadError.Http;
            } else if (e instanceof DownloadCancelException)
            {
                error = DownloadError.Cancel;
            }

            FDownloadManager.this.notifyError(mInfo, error);
        }
    }

    private final class DownloadInfoWrapper
    {
        private final DownloadInfo mDownloadInfo;
        private final File mTempFile;

        public DownloadInfoWrapper(DownloadInfo downloadInfo, File tempFile)
        {
            if (downloadInfo == null)
                throw new IllegalArgumentException("downloadInfo is null");

            if (tempFile == null)
                throw new IllegalArgumentException("tempFile is null");

            mDownloadInfo = downloadInfo;
            mTempFile = tempFile;
        }
    }
}
