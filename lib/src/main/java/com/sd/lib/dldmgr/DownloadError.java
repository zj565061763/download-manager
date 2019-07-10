package com.sd.lib.dldmgr;

public enum DownloadError
{
    /**
     * 创建文件失败
     */
    CreateFile,
    /**
     * 提交下载任务失败
     */
    SubmitFailed,
    /**
     * 下载文件不存在
     */
    DownloadFileNotExists,
    /**
     * 重命名文件失败
     */
    RenameFile,
    /**
     * Http请求异常
     */
    Http,
    /**
     * 其他未知异常
     */
    Other
}
