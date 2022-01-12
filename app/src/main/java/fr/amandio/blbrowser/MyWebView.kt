package fr.amandio.blbrowser

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.webkit.WebView

class MyWebView constructor(context: Context, attributeSet: AttributeSet? = null): WebView( context, attributeSet) {

    init {
        Log.v(TAG, "init")
    }

    override fun onDraw(canvas: Canvas?) {
        Log.v(TAG, "onDraw")
        super.onDraw(canvas)
    }

    override fun loadData(data: String, mimeType: String?, encoding: String?) {
        Log.v(TAG, "loadData data:$data")
        super.loadData(data, mimeType, encoding)
    }

    override fun loadDataWithBaseURL(baseUrl: String?, data: String, mimeType: String?, encoding: String?, historyUrl: String?) {
        Log.v(TAG, "loadDataWithBaseURL data:$data")
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)
    }

    override fun loadUrl(url: String) {
        Log.v(TAG, "loadDataWithBaseURL url:$url")
        super.loadUrl(url)
    }


    companion object {
        const val TAG = "MyWebView"
    }
}
