package fr.amandio.blbrowser

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.nav_header_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var firstTimeUpdateSucceeded = AUTO_HIDE
    private var homeUrlEditText: EditText? = null
    private var mDrawerLayout: DrawerLayout? = null

    var mProgressBar: ProgressBar? = null
    var navigationViewHeaderView: View? = null
    var webView: WebView? = null

    var mDateTimeHandler = Handler()
    private var mDateTimeHandlerTask: Runnable = object : Runnable {
        override fun run() {
            updateDateTime()
            mDateTimeHandler.postDelayed(this, INTERVAL)
        }
    }
    private val mHideHandler = Handler()
    @SuppressLint("SetJavaScriptEnabled")
    private val mHideRunnable = Runnable {
        webView?.settings?.javaScriptEnabled = true
        webView?.settings?.builtInZoomControls = AUTO_HIDE
        webView?.settings?.displayZoomControls = false
        webView?.settings?.loadWithOverviewMode = AUTO_HIDE
        webView?.settings?.useWideViewPort = AUTO_HIDE
        webView?.loadUrl(lastUrl)
    }

    fun updateDateTime() {
        val textView = findViewById<TextView>(R.id.clockText)
        if (textView != null) {
            textView.text = SimpleDateFormat(DATE_FORMAT, Locale.FRANCE).format(Date())
            if (firstTimeUpdateSucceeded) {
                firstTimeUpdateSucceeded = false
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(MY_PREFS_NAME, 0)
        if (prefs.getString(PREFS_HOME, null) != null) {
            homeUrl = prefs.getString(PREFS_HOME, homeUrl)
        }
        lastUrl = homeUrl
        window.requestFeature(9)
        window.requestFeature(1)
        window.addFlags(2097152)
        window.addFlags(1024)
        window.addFlags(33554432)
        setContentView(R.layout.activity_main)
        mDrawerLayout = findViewById(R.id.drawer_layout)
        webView = findViewById(R.id.webView)
        val webSettings = webView?.settings
        webSettings?.setSupportZoom(AUTO_HIDE)
        webSettings?.setAppCacheEnabled(false)
        LayoutInflater.from(baseContext).inflate(R.layout.nav_header_main, null)
        val navigationView = findViewById<View>(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)
        navigationViewHeaderView = navigationView.getHeaderView(0)
        val seekBarLight =
            navigationViewHeaderView?.findViewById<View>(R.id.seekBarLight) as SeekBar
        seekBarLight.max = 100
        seekBarLight.progress = 100
        seekBarLight.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val alpha = (progress * progress).toFloat() * 0.9f / 10000.0f + 0.1f
                webView?.alpha = alpha
                navigationViewHeaderView?.alpha = alpha
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        (navigationViewHeaderView?.findViewById<View>(R.id.googleEditText) as EditText).setOnEditorActionListener { _, actionId, _ ->
            Log.v(TAG, "OnEditorActionListener $actionId")
            if (actionId == 6 || actionId == 5) {
                mDrawerLayout?.closeDrawer(GravityCompat.START)
                goFullScreen()
                execGoogleSearch()
            }
            false
        }
        (navigationViewHeaderView?.findViewById<View>(R.id.httpEditText) as EditText).setOnEditorActionListener { _, actionId, _ ->
            Log.v(TAG, "OnEditorActionListener $actionId")
            if (actionId == 6 || actionId == 5) {
                mDrawerLayout?.closeDrawer(GravityCompat.START)
                goFullScreen()
                execOpenUrl()
            }
            false
        }
        webView?.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                if (url == homeUrl || clearHistory) {
                    view.clearHistory()
                    view.clearCache(AUTO_HIDE)
                    view.clearFormData()
                    clearHistory = false
                }
                Log.v(TAG, "loadUrl $url")
                super.onPageFinished(view, url)
                lastUrl = url
                val urlEditText = findViewById<View>(R.id.httpEditText) as EditText
                urlEditText.setText(url)
                updateDateTime()
            }
        }
        mProgressBar = findViewById(R.id.progressBar)
        webView?.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                Log.v(TAG, "onProgressChanged progress:$progress")
                if (mProgressBar?.visibility == View.GONE && lastUrl!!.indexOf("showphoto") < 0) {
                    mProgressBar?.visibility = View.VISIBLE
                }
                mProgressBar?.progress = progress
                if (progress == 100) {
                    mProgressBar?.visibility = View.GONE
                }
            }
        }
        navigationViewHeaderView!!.findViewById<View>(R.id.btnBack).setOnClickListener {
            webView?.goBack()
            mDrawerLayout?.closeDrawer(GravityCompat.START)
        }
        navigationViewHeaderView!!.findViewById<View>(R.id.btnSettings).setOnClickListener {
            mDrawerLayout?.closeDrawer(GravityCompat.START)
            goFullScreen()
            homeUrlEditText = EditText(webView?.context)
            homeUrlEditText?.setText(homeUrl)
            homeUrlEditText?.inputType = 144
            val builder = AlertDialog.Builder(webView?.context)
            builder.setTitle("HomePage").setView(homeUrlEditText)
                .setPositiveButton("Ok") { dialog, _ ->
                    homeUrl = homeUrlEditText?.text.toString()
                    val editor = getSharedPreferences(MY_PREFS_NAME, 0).edit()
                    editor.putString(PREFS_HOME, homeUrl)
                    editor.apply()
                    dialog.dismiss()
                    goFullScreen()
                }.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    goFullScreen()
                }
            builder.show()
        }
        navigationViewHeaderView!!.findViewById<View>(R.id.btnRefresh).setOnClickListener {
            mDrawerLayout?.closeDrawer(GravityCompat.START)
            goFullScreen()
            webView?.reload()
        }
        navigationViewHeaderView!!.findViewById<View>(R.id.btnCancel).setOnClickListener {
            mDrawerLayout?.closeDrawer(GravityCompat.START)
            goFullScreen()
            webView?.stopLoading()
        }
        navigationViewHeaderView!!.findViewById<View>(R.id.btnHome).setOnClickListener {
            mDrawerLayout?.closeDrawer(GravityCompat.START)
            goFullScreen()
            webView?.loadUrl(homeUrl)
            clearHistory = AUTO_HIDE
        }
        navigationViewHeaderView!!.findViewById<View>(R.id.btnExit).setOnClickListener {
            mDrawerLayout?.closeDrawer(GravityCompat.START)
            goFullScreen()
            finish()
        }
        navigationViewHeaderView!!.findViewById<View>(R.id.btnLight).setOnClickListener {
            when {
                seekBarLight.progress == 0 -> seekBarLight.progress = 100
                seekBarLight.progress == 100 -> seekBarLight.progress = 0
                seekBarLight.progress < 50 -> seekBarLight.progress = 0
                else -> seekBarLight.progress = 100
            }
        }
        goFullScreen()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        webView?.clearCache(AUTO_HIDE)
    }

    public override fun onStart() {
        super.onStart()
        startRepeatingTask()
        updateDateTime()
    }

    public override fun onStop() {
        super.onStop()
        stopRepeatingTask()
        webView?.clearCache(AUTO_HIDE)
        webView?.clearFormData()
    }

    private fun startRepeatingTask() {
        mDateTimeHandlerTask.run()
    }

    private fun stopRepeatingTask() {
        mDateTimeHandler.removeCallbacks(mDateTimeHandlerTask)
    }

    public override fun onPause() {
        super.onPause()
        webView?.onPause()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView?.saveState(outState)
        m_savedInstanceState = outState
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView?.restoreState(savedInstanceState)
    }

    public override fun onResume() {
        super.onResume()
        updateDateTime()
        goFullScreen()
        if (m_savedInstanceState == null) {
            webView?.loadUrl(lastUrl)
        }
        webView?.onResume()
    }

    public override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        delayedHide()
    }

    private fun delayedHide() {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, DELAY_HIDE)
    }

    private fun goFullScreen() {
        Log.v(TAG, "goFullScreen")
        val decorView = window.decorView
        decorView.systemUiVisibility = 4358
        val currentFocusView = currentFocus
        if (currentFocusView != null) {
            currentFocusView.clearFocus()
            currentFocusView.requestFocus()
            Log.v(TAG, "goFullScreen : focus back to currentFocus")
            return
        }
        decorView.clearFocus()
        decorView.requestFocus()
        Log.v(TAG, "goFullScreen : no view has focus")
    }

    private fun execGoogleSearch() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            currentFocus?.windowToken,
            0
        )
        webView?.loadUrl("https://search.lilo.org/?q=" + googleEditText.text)
        googleEditText.setText("")
    }

    private fun execOpenUrl() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            currentFocus?.windowToken,
            0
        )
        val url =
            httpEditText.text.replace("http://".toRegex(), "").replace("https://".toRegex(), "")
        webView?.loadUrl("https://$url")
    }

    override fun onBackPressed() {
        if (webView?.url != homeUrl) {
            webView?.goBack()
            mDrawerLayout?.closeDrawer(GravityCompat.START)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return AUTO_HIDE
    }

    companion object {
        val TAG = MainActivity::class.java.simpleName

        private const val AUTO_HIDE = true
        private const val INTERVAL = 60000L
        private const val DELAY_HIDE = 300L
        private const val PREFS_HOME = "HOME"
        private const val MY_PREFS_NAME = "BLBrowserPreferences"
        private const val DATE_FORMAT = "E dd       HH:mm"

        var clearHistory = false
        var homeUrl: String? = "https://amandio.fr"
        var lastUrl: String? = null
        private var m_savedInstanceState: Bundle? = null
    }
}
