package com.sd.lib.dldmgr.utils

import com.sd.lib.dldmgr.IDownloadManager
import java.util.concurrent.ConcurrentHashMap

internal class UrlCallbackHolder {
    private val _mapCallback: MutableMap<String, MutableMap<IDownloadManager.Callback, String>> = HashMap()
    private val _mapCallbackUrl: MutableMap<IDownloadManager.Callback, String> = HashMap()

    @Synchronized
    fun add(url: String, callback: IDownloadManager.Callback) {
        var holder = _mapCallback[url]
        if (holder == null) {
            holder = ConcurrentHashMap()
            _mapCallback[url] = holder
        }

        if (holder.put(callback, "") == null) {
            _mapCallbackUrl[callback] = url
        }
    }

    @Synchronized
    fun remove(callback: IDownloadManager.Callback): Boolean {
        val url = _mapCallbackUrl.remove(callback) ?: return false
        var holder = _mapCallback[url]
        return holder?.remove(callback) != null
    }

    @Synchronized
    fun removeUrl(url: String): Array<IDownloadManager.Callback>? {
        var holder = _mapCallback.remove(url) ?: return null
        return holder.keys.toTypedArray()
    }
}