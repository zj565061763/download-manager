package com.sd.lib.dldmgr.executor;

import com.sd.lib.dldmgr.DownloadRequest;
import com.sd.lib.dldmgr.updater.DownloadUpdater;

import java.io.File;

public interface DownloadExecutor
{
    /**
     * 提交下载任务
     *
     * @param updater 下载信息更新对象
     * @param file    要保存的下载文件
     * @param request 下载请求
     * @return true-提交成功，false-提交失败
     */
    boolean submit(DownloadUpdater updater, File file, DownloadRequest request);
}
