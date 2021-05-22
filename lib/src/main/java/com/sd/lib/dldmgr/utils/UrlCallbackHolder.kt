package com.sd.lib.dldmgr.utils

import com.sd.lib.dldmgr.IDownloadManager

internal class UrlCallbackHolder {
    private val _mapCallback: MutableMap<String, MutableSet<IDownloadManager.Callback>> = HashMap()
    private val _mapCallbackUrl: MutableMap<IDownloadManager.Callback, String> = HashMap()

    @Synchronized
    fun add(url: String, callback: IDownloadManager.Callback) {
        var holder = _mapCallback[url]
        if (holder == null) {
            holder = HashSet()
            _mapCallback[url] = holder
        }

        if (holder.add(callback)) {
            _mapCallbackUrl[callback] = url
        }
    }

    @Synchronized
    fun remove(callback: IDownloadManager.Callback): Boolean {
        val url = _mapCallbackUrl.remove(callback) ?: return false
        var holder = _mapCallback[url] ?: return false

        val remove = holder.remove(callback)
        if (holder.isEmpty()) {
            _mapCallback.remove(url)
        }
        return remove
    }

    @Synchronized
    fun removeUrl(url: String): Array<IDownloadManager.Callback>? {
        var holder = _mapCallback.remove(url) ?: return null
        return holder.toTypedArray()
    }
}