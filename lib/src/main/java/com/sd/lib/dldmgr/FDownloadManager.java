package com.sd.lib.dldmgr;

import android.text.TextUtils;
import android.util.Log;

import com.sd.lib.dldmgr.exception.DownloadHttpException;

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
                    final String directory = getConfig().getDownloadDirectory();
                    sDefault = new FDownloadManager(directory);
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
        return Utils.checkDir(mDirectory);
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

        final File file = newDownloadFile(url);
        if (file == null)
            return null;

        return file.exists() ? file : null;
    }

    @Override
    public File getTempFile(String url)
    {
        if (TextUtils.isEmpty(url))
            return null;

        final File file = newTempFile(url);
        if (file == null)
            return null;

        return file.exists() ? file : null;
    }

    private File newTempFile(String url)
    {
        return newUrlFile(url, EXT_TEMP);
    }

    private File newDownloadFile(String url)
    {
        final String ext = Utils.getExt(url);
        return newUrlFile(url, ext);
    }

    private synchronized File newUrlFile(String url, String ext)
    {
        if (TextUtils.isEmpty(url))
            throw new IllegalArgumentException("url is empty");

        if (!checkDirectory())
            return null;

        if (TextUtils.isEmpty(ext))
        {
            ext = "";
        } else
        {
            if (!ext.startsWith("."))
                ext = "." + ext;
        }

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
    }

    @Override
    public synchronized void deleteDownloadFile(String ext)
    {
        if (!TextUtils.isEmpty(ext))
        {
            if (ext.startsWith("."))
                throw new IllegalArgumentException("Illegal ext start with dot:" + ext);
        }

        final File[] files = getAllFile();
        if (files == null || files.length <= 0)
            return;

        int count = 0;
        boolean delete = false;
        for (File item : files)
        {
            final String name = item.getName();
            if (name.endsWith(EXT_TEMP))
                continue;

            if (ext == null)
            {
                // 删除所有下载文件
                delete = true;
            } else
            {
                final String itemExt = Utils.getExt(item.getAbsolutePath());
                if (ext.isEmpty())
                {
                    // 删除扩展名为空的下载文件
                    if (TextUtils.isEmpty(itemExt))
                        delete = true;
                } else if (ext.equals(itemExt))
                {
                    // 删除指定扩展名的文件
                    delete = true;
                }
            }

            if (delete)
            {
                if (item.delete())
                    count++;
            }
        }

        if (getConfig().isDebug())
            Log.i(TAG, "deleteDownloadFile count:" + count + " ext:" + ext);
    }

    @Override
    public boolean addTask(final String url)
    {
        final DownloadRequest downloadRequest = DownloadRequest.url(url);
        return addTask(downloadRequest);
    }

    @Override
    public boolean addTask(DownloadRequest request)
    {
        return addTask(request, null);
    }

    @Override
    public synchronized boolean addTask(DownloadRequest request, Callback callback)
    {
        if (request == null)
            return false;

        final String url = request.getUrl();
        if (TextUtils.isEmpty(url))
            return false;

        if (mMapDownloadInfo.containsKey(url))
        {
            addCallback(callback);
            return true;
        }

        final DownloadInfo info = new DownloadInfo(url);

        final File tempFile = newTempFile(url);
        if (tempFile == null)
        {
            if (getConfig().isDebug())
                Log.e(TAG, "addTask error create temp file error:" + url);

            notifyError(info, DownloadError.CreateTempFile);
            if (callback != null)
                callback.onError(info);

            return false;
        }

        final DownloadUpdater downloadUpdater = new InternalDownloadUpdater(info, tempFile);
        final boolean submitted = getConfig().getDownloadExecutor().submit(request, tempFile, downloadUpdater);
        if (submitted)
        {
            final DownloadInfoWrapper wrapper = new DownloadInfoWrapper(info, tempFile);
            mMapDownloadInfo.put(url, wrapper);
            mMapTempFile.put(tempFile, url);

            if (getConfig().isDebug())
            {
                Log.i(TAG, "addTask"
                        + " url:" + url
                        + " path:" + tempFile.getAbsolutePath()
                        + " size:" + mMapDownloadInfo.size()
                        + " tempSize:" + mMapTempFile.size());
            }

            addCallback(callback);
            notifyPrepare(info);

        } else
        {
            if (getConfig().isDebug())
                Log.e(TAG, "addTask error submit request failed:" + url);

            notifyError(info, DownloadError.SubmitFailed);
            if (callback != null)
                callback.onError(info);
        }

        return submitted;
    }

    @Override
    public synchronized boolean cancelTask(String url)
    {
        if (TextUtils.isEmpty(url))
            return false;

        if (getConfig().isDebug())
        {
            Log.i(TAG, "cancelTask start"
                    + " url:" + url);
        }

        final boolean result = getConfig().getDownloadExecutor().cancel(url);

        if (getConfig().isDebug())
        {
            Log.i(TAG, "cancelTask finish"
                    + " result:" + result
                    + " url:" + url);
        }

        return result;
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
    }

    private void notifyError(DownloadInfo info, DownloadError error)
    {
        notifyError(info, error, null);
    }

    private void notifyError(DownloadInfo info, DownloadError error, Throwable throwable)
    {
        info.setState(DownloadState.Error);
        info.setError(error);
        info.setThrowable(throwable);
        mMainThreadCallback.onError(info);
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
            {
                Log.i(TAG, "removeDownloadInfo"
                        + " url:" + url
                        + " size:" + mMapDownloadInfo.size()
                        + " tempSize:" + mMapTempFile.size());
            }
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
            if (info.getTransmitParam().getProgress() == 1)
            {
                Log.i(TAG, "onProgress"
                        + " url:" + info.getUrl()
                        + " progress:" + 1);
            }

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
                    if (getConfig().isDebug())
                    {
                        Log.i(TAG, "notify callback onSuccess"
                                + " url:" + info.getUrl()
                                + " file:" + file.getAbsolutePath());
                    }

                    synchronized (FDownloadManager.this)
                    {
                        // 移除下载信息
                        removeDownloadInfo(info.getUrl());

                        for (Callback item : mListCallback)
                        {
                            item.onSuccess(info, file);
                        }
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
                    if (getConfig().isDebug())
                    {
                        Log.i(TAG, "notify callback onError"
                                + " url:" + info.getUrl()
                                + " error:" + info.getError());
                    }

                    synchronized (FDownloadManager.this)
                    {
                        // 移除下载信息
                        removeDownloadInfo(info.getUrl());

                        for (Callback item : mListCallback)
                        {
                            item.onError(info);
                        }
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
                Log.i(TAG, DownloadUpdater.class.getSimpleName() + " download success:" + mUrl);

            if (!mTempFile.exists())
            {
                if (getConfig().isDebug())
                    Log.e(TAG, DownloadUpdater.class.getSimpleName() + " download success error temp file not exists:" + mUrl);

                FDownloadManager.this.notifyError(mInfo, DownloadError.TempFileNotExists);
                return;
            }

            final File downloadFile = newDownloadFile(mUrl);
            if (downloadFile == null)
            {
                if (getConfig().isDebug())
                    Log.e(TAG, DownloadUpdater.class.getSimpleName() + " download success error create download file:" + mUrl);

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
                    Log.e(TAG, DownloadUpdater.class.getSimpleName() + " download success error rename temp file to download file:" + mUrl);

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
                Log.e(TAG, DownloadUpdater.class.getSimpleName() + " download error:" + mUrl + " " + e);

            DownloadError error = DownloadError.Other;
            if (e instanceof DownloadHttpException)
            {
                error = DownloadError.Http;
            }

            FDownloadManager.this.notifyError(mInfo, error, e);
        }

        @Override
        public void notifyCancel()
        {
            if (mCompleted)
                return;

            mCompleted = true;

            if (getConfig().isDebug())
                Log.i(TAG, DownloadUpdater.class.getSimpleName() + " download cancel:" + mUrl);

            FDownloadManager.this.notifyError(mInfo, DownloadError.Cancel);
        }
    }

    private static final class DownloadInfoWrapper
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
