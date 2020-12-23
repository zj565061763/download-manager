package com.sd.lib.dldmgr;

import android.text.TextUtils;

import java.io.File;

public class DownloadDirectory implements IDownloadDirectory
{
    private final File mDirectory;

    private DownloadDirectory(File directory)
    {
        // 此处不检查目录对象是否为null
        mDirectory = directory;
    }

    public static DownloadDirectory from(File directory)
    {
        return new DownloadDirectory(directory);
    }

    @Override
    public boolean checkExist()
    {
        return Utils.checkDir(mDirectory);
    }

    @Override
    public File getFile(String url)
    {
        final File file = newUrlFile(url);
        if (file == null)
            return null;

        return file.exists() ? file : null;
    }

    @Override
    public File getFile(String url, File defaultFile)
    {
        final File file = getFile(url);
        return file != null ? file : defaultFile;
    }

    @Override
    public File getTempFile(String url)
    {
        final File file = newUrlTempFile(url);
        if (file == null)
            return null;

        return file.exists() ? file : null;
    }

    File newUrlFile(String url)
    {
        if (TextUtils.isEmpty(url))
            return null;

        final String ext = Utils.getExt(url);
        return createUrlFile(url, ext);
    }

    File newUrlTempFile(String url)
    {
        if (TextUtils.isEmpty(url))
            return null;

        final String ext = EXT_TEMP;
        return createUrlFile(url, ext);
    }

    @Override
    public synchronized File copyFile(File file)
    {
        final File directory = mDirectory;
        if (!Utils.checkDir(directory))
            return file;

        if (file == null || !file.exists())
            return file;

        if (file.isDirectory())
            throw new IllegalArgumentException("file must not be a directory");

        final String filename = file.getName();
        final File tempFile = new File(directory, filename + EXT_TEMP);
        if (Utils.copyFile(file, tempFile))
        {
            final File copyFile = new File(directory, filename);
            Utils.delete(copyFile);

            if (tempFile.renameTo(copyFile))
                return copyFile;
        }
        return file;
    }

    @Override
    public File takeFile(File file)
    {
        final File directory = mDirectory;
        if (!Utils.checkDir(directory))
            return file;

        if (file == null || !file.exists())
            return file;

        if (file.isDirectory())
            throw new IllegalArgumentException("file must not be a directory");

        final String filename = file.getName();
        final File newFile = new File(directory, filename);
        if (Utils.moveFile(file, newFile))
            return newFile;

        return file;
    }

    @Override
    public synchronized int deleteFile(String ext)
    {
        if (!TextUtils.isEmpty(ext))
        {
            if (ext.startsWith("."))
                throw new IllegalArgumentException("Illegal ext start with dot:" + ext);
        }

        final File[] files = getAllFile();
        if (files == null || files.length <= 0)
            return 0;

        int count = 0;
        boolean delete = false;
        for (File file : files)
        {
            final String name = file.getName();
            if (name.endsWith(EXT_TEMP))
                continue;

            if (ext == null)
            {
                delete = true;
            } else
            {
                final String itemExt = Utils.getExt(file.getAbsolutePath());
                if (ext.equals(itemExt))
                    delete = true;
            }

            if (delete)
            {
                if (Utils.delete(file))
                    count++;
            }
        }
        return count;
    }

    @Override
    public synchronized int deleteTempFile(FileInterceptor interceptor)
    {
        final File[] files = getAllFile();
        if (files == null || files.length <= 0)
            return 0;

        int count = 0;
        for (File file : files)
        {
            if (interceptor != null && interceptor.intercept(file))
                continue;

            final String name = file.getName();
            if (name.endsWith(EXT_TEMP))
            {
                if (Utils.delete(file))
                    count++;
            }
        }
        return count;
    }

    private File[] getAllFile()
    {
        final File directory = mDirectory;
        if (!Utils.checkDir(directory))
            return null;

        final File[] files = directory.listFiles();
        if (files == null || files.length <= 0)
            return null;

        return files;
    }

    private synchronized File createUrlFile(String url, String ext)
    {
        if (TextUtils.isEmpty(url))
            return null;

        final File directory = mDirectory;
        if (!Utils.checkDir(directory))
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
        return new File(directory, fileName);
    }

    @Override
    public int hashCode()
    {
        return mDirectory != null ? mDirectory.hashCode() : super.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (obj == null) return false;
        if (obj.getClass() != getClass()) return false;

        if (mDirectory == null)
            return super.equals(obj);

        final DownloadDirectory other = (DownloadDirectory) obj;
        return mDirectory.equals(other.mDirectory);
    }

    @Override
    public String toString()
    {
        return "{" +
                "mDirectory=" + mDirectory +
                " hash=" + super.toString() +
                "}";
    }
}
