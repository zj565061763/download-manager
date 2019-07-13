package com.sd.lib.dldmgr;

public enum DownloadError
{
    /**
     * 获取临时文件失败
     */
    CreateTempFile,
    /**
     * 提交下载任务失败
     */
    SubmitFailed,
    /**
     * 临时文件文件不存在
     */
    TempFileNotExists,
    /**
     * 获取下载文件失败
     */
    CreateDownloadFile,
    /**
     * 临时文件重命名为下载文件失败
     */
    RenameFile,
    /**
     * Http请求异常
     */
    Http,
    /**
     * 下载被取消
     */
    Cancel,
    /**
     * 其他未知异常
     */
    Other
}
