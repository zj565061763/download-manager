package com.sd.lib.dldmgr;

public enum DownloadError
{
    /**
     * 创建文件失败
     */
    CreateFile,
    /**
     * 重命名文件失败
     */
    RenameFile,
    /**
     * Http请求异常
     */
    Http
}
