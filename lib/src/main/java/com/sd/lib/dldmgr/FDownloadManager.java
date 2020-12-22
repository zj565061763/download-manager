package com.sd.lib.dldmgr;

import android.text.TextUtils;
import android.util.Log;

import com.sd.lib.dldmgr.exception.DownloadHttpException;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FDownloadManager implements IDownloadManager
{
    private static FDownloadManager sDefault = null;

    private final DownloadDirectory mDownloadDirectory;

    private final Map<String, DownloadInfoWrapper> mMapDownloadInfo = new ConcurrentHashMap<>();
    private final Map<File, String> mMapTempFile = new ConcurrentHashMap<>();

    private final Map<Callback, String> mCallbackHolder = new ConcurrentHashMap<>();
    private final Map<String, Map<FileProcessor, String>> mProcessorHolder = new ConcurrentHashMap<>();

    protected FDownloadManager(String directory)
    {
        if (TextUtils.isEmpty(directory))
            throw new IllegalArgumentException("directory is empty");

        mDownloadDirectory = DownloadDirectory.from(new File(directory));
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

    @Override
    public synchronized void addCallback(Callback callback)
    {
        if (callback == null)
            return;

        final String put = mCallbackHolder.put(callback, "");
        if (put == null)
        {
            if (getConfig().isDebug())
                Log.i(TAG, "addCallback:" + callback + " size:" + mCallbackHolder.size());
        }
    }

    @Override
    public synchronized void removeCallback(Callback callback)
    {
        if (callback == null)
            return;

        final String remove = mCallbackHolder.remove(callback);
        if (remove != null)
        {
            if (getConfig().isDebug())
                Log.i(TAG, "removeCallback:" + callback + " size:" + mCallbackHolder.size());
        }
    }

    @Override
    public File getDownloadFile(String url)
    {
        return mDownloadDirectory.getFile(url);
    }

    @Override
    public File getTempFile(String url)
    {
        return mDownloadDirectory.getTempFile(url);
    }

    @Override
    public DownloadInfo getDownloadInfo(String url)
    {
        final DownloadInfoWrapper wrapper = mMapDownloadInfo.get(url);
        if (wrapper == null)
            return null;

        return wrapper.mDownloadInfo;
    }

    @Override
    public void deleteTempFile()
    {
        final int count = mDownloadDirectory.deleteTempFile(new IDownloadDirectory.FileInterceptor()
        {
            @Override
            public boolean intercept(File file)
            {
                if (mMapTempFile.containsKey(file))
                    return true;
                return false;
            }
        });

        if (getConfig().isDebug())
            Log.i(TAG, "deleteTempFile count:" + count);
    }

    @Override
    public void deleteDownloadFile(String ext)
    {
        final int count = mDownloadDirectory.deleteFile(ext);

        if (getConfig().isDebug())
            Log.i(TAG, "deleteDownloadFile count:" + count + " ext:" + ext);
    }

    @Override
    public synchronized boolean addFileProcessor(String url, FileProcessor processor)
    {
        if (TextUtils.isEmpty(url) || processor == null)
            return false;

        final DownloadInfo downloadInfo = getDownloadInfo(url);
        if (downloadInfo == null)
            return false;

        if (downloadInfo.getState().isCompleted())
            return false;

        Map<FileProcessor, String> map = mProcessorHolder.get(url);
        if (map == null)
        {
            map = new ConcurrentHashMap<>();
            mProcessorHolder.put(url, map);
        }

        final String put = map.put(processor, "");
        if (put == null)
        {
            if (getConfig().isDebug())
            {
                Log.i(TAG, "addFileProcessor url:" + url
                        + " processor:" + processor
                        + " size:" + map.size()
                        + " totalSize:" + mProcessorHolder.size()
                );
            }
        }
        return true;
    }

    @Override
    public synchronized void removeFileProcessor(String url, FileProcessor processor)
    {
        if (TextUtils.isEmpty(url) || processor == null)
            return;

        final Map<FileProcessor, String> map = mProcessorHolder.get(url);
        if (map == null)
            return;

        final String remove = map.remove(processor);
        if (remove != null)
        {
            if (map.isEmpty())
                mProcessorHolder.remove(url);

            if (getConfig().isDebug())
            {
                Log.i(TAG, "removeFileProcessor url:" + url
                        + " size:" + map.size()
                        + " totalSize:" + mProcessorHolder.size()
                );
            }
        }
    }

    @Override
    public synchronized void clearFileProcessor(String url)
    {
        if (TextUtils.isEmpty(url))
            return;

        final Map<FileProcessor, String> map = mProcessorHolder.remove(url);
        if (map != null)
        {
            map.clear();
            if (getConfig().isDebug())
            {
                Log.i(TAG, "clearFileProcessor url:" + url
                        + " totalSize:" + mProcessorHolder.size()
                );
            }
        }
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

        final File tempFile = mDownloadDirectory.newUrlTempFile(url);
        if (tempFile == null)
        {
            if (getConfig().isDebug())
                Log.e(TAG, "addTask error create temp file error:" + url);

            notifyError(info, DownloadError.CreateTempFile);
            if (callback != null)
                callback.onError(info);

            return false;
        }

        final IDownloadUpdater downloadUpdater = new InternalDownloadUpdater(info, tempFile);
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

    private void notifyPrepare(DownloadInfo info)
    {
        info.setState(DownloadState.Prepare);

        final DownloadInfo copyInfo = info.copy();
        Utils.runOnMainThread(new Runnable()
        {
            @Override
            public void run()
            {
                final Collection<Callback> callbacks = mCallbackHolder.keySet();
                for (Callback item : callbacks)
                {
                    item.onPrepare(copyInfo);
                }
            }
        });
    }

    private void notifyProgress(DownloadInfo info, long total, long current)
    {
        info.setState(DownloadState.Downloading);

        final boolean changed = info.getTransmitParam().transmit(total, current);
        if (changed)
        {
            final DownloadInfo copyInfo = info.copy();
            Utils.runOnMainThread(new Runnable()
            {
                @Override
                public void run()
                {
                    final Collection<Callback> callbacks = mCallbackHolder.keySet();
                    for (Callback item : callbacks)
                    {
                        item.onProgress(copyInfo);
                    }
                }
            });
        }
    }

    private void notifySuccess(DownloadInfo info, final File file)
    {
        info.setState(DownloadState.Success);
        clearFileProcessor(info.getUrl());

        final DownloadInfo copyInfo = info.copy();
        Utils.runOnMainThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (getConfig().isDebug())
                {
                    Log.i(TAG, "notify callback onSuccess"
                            + " url:" + copyInfo.getUrl()
                            + " file:" + file.getAbsolutePath());
                }

                synchronized (FDownloadManager.this)
                {
                    // 移除下载信息
                    removeDownloadInfo(copyInfo.getUrl());

                    final Collection<Callback> callbacks = mCallbackHolder.keySet();
                    for (Callback item : callbacks)
                    {
                        item.onSuccess(copyInfo, file);
                    }
                }
            }
        });
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
        clearFileProcessor(info.getUrl());

        final DownloadInfo copyInfo = info.copy();
        Utils.runOnMainThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (getConfig().isDebug())
                {
                    Log.i(TAG, "notify callback onError"
                            + " url:" + copyInfo.getUrl()
                            + " error:" + copyInfo.getError());
                }

                synchronized (FDownloadManager.this)
                {
                    // 移除下载信息
                    removeDownloadInfo(copyInfo.getUrl());

                    final Collection<Callback> callbacks = mCallbackHolder.keySet();
                    for (Callback item : callbacks)
                    {
                        item.onError(copyInfo);
                    }
                }
            }
        });
    }

    private final class InternalDownloadUpdater implements IDownloadUpdater
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
                Log.i(TAG, IDownloadUpdater.class.getSimpleName() + " download success:" + mUrl);

            if (!mTempFile.exists())
            {
                if (getConfig().isDebug())
                    Log.e(TAG, IDownloadUpdater.class.getSimpleName() + " download success error temp file not exists:" + mUrl);

                FDownloadManager.this.notifyError(mInfo, DownloadError.TempFileNotExists);
                return;
            }

            final File downloadFile = mDownloadDirectory.newUrlFile(mUrl);
            if (downloadFile == null)
            {
                if (getConfig().isDebug())
                    Log.e(TAG, IDownloadUpdater.class.getSimpleName() + " download success error create download file:" + mUrl);

                FDownloadManager.this.notifyError(mInfo, DownloadError.CreateDownloadFile);
                return;
            }

            if (downloadFile.exists())
                downloadFile.delete();

            if (mTempFile.renameTo(downloadFile))
            {
                processDownloadFile(downloadFile);
                FDownloadManager.this.notifySuccess(mInfo, downloadFile);
            } else
            {
                if (getConfig().isDebug())
                    Log.e(TAG, IDownloadUpdater.class.getSimpleName() + " download success error rename temp file to download file:" + mUrl);

                FDownloadManager.this.notifyError(mInfo, DownloadError.RenameFile);
            }
        }

        private void processDownloadFile(File downloadFile)
        {
            synchronized (FDownloadManager.this)
            {
                final String url = mUrl;
                final Map<FileProcessor, String> map = mProcessorHolder.get(url);
                if (map == null)
                    return;

                for (FileProcessor processor : map.keySet())
                {
                    processor.process(downloadFile);
                }

                if (getConfig().isDebug())
                {
                    Log.i(TAG, "processDownloadFile finish:" + url
                            + " size:" + map.size()
                            + " totalSize:" + mProcessorHolder.size()
                    );
                }
            }
        }

        @Override
        public void notifyError(Exception e, String details)
        {
            if (mCompleted)
                return;

            mCompleted = true;

            if (getConfig().isDebug())
                Log.e(TAG, IDownloadUpdater.class.getSimpleName() + " download error:" + mUrl + " " + e);

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
                Log.i(TAG, IDownloadUpdater.class.getSimpleName() + " download cancel:" + mUrl);

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
