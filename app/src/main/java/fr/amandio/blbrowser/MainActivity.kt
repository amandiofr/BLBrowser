package fr.amandio.blbrowser

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import fr.amandio.blbrowser.databinding.ActivityMainBinding
import fr.amandio.blbrowser.databinding.NavHeaderMainBinding
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var firstTimeUpdateSucceeded = true
    private var homeUrlEditText: EditText? = null

    private lateinit var activityMainBinding: ActivityMainBinding
    private lateinit var navHeaderMainBinding: NavHeaderMainBinding

    var mDateTimeHandler = Handler(Looper.getMainLooper())
    private var mDateTimeHandlerTask: Runnable = object : Runnable {
        override fun run() {
            updateDateTime()
            mDateTimeHandler.postDelayed(this, INTERVAL)
        }
    }
    private val mHideHandler = Handler(Looper.getMainLooper())
    private val mHideRunnable = Runnable {
        activityMainBinding.webView.settings.javaScriptEnabled = true
        activityMainBinding.webView.settings.builtInZoomControls = true
        activityMainBinding.webView.settings.displayZoomControls = false
        activityMainBinding.webView.settings.loadWithOverviewMode = true
        activityMainBinding.webView.settings.useWideViewPort = true
        lastUrl?.let { activityMainBinding.webView.loadUrl(it) }
    }

    fun updateDateTime() {
        navHeaderMainBinding.clockText.text = SimpleDateFormat(DATE_FORMAT, Locale.FRANCE).format(Date())
        if (firstTimeUpdateSucceeded) {
            firstTimeUpdateSucceeded = false
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.v(TAG, "onCreate")

        val prefs = getSharedPreferences(MY_PREFS_NAME, 0)
        if (prefs.getString(PREFS_HOME, null) != null) {
            homeUrl = prefs.getString(PREFS_HOME, homeUrl)
        }
        lastUrl = homeUrl
        with(window) {
            requestFeature(9)
            requestFeature(1)
            addFlags(2097152)
            addFlags(1024)
            addFlags(33554432)
        }

        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        activityMainBinding.webView.settings.setSupportZoom(true)
        activityMainBinding.navView.setNavigationItemSelectedListener(this)
        val navigationViewHeaderView = activityMainBinding.navView.getHeaderView(0)
        navHeaderMainBinding = NavHeaderMainBinding.bind(navigationViewHeaderView)
        with(navHeaderMainBinding.seekBarLight) {
            max = 100
            progress = 100
            setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val alpha = (progress * progress).toFloat() * 0.9f / 10000.0f + 0.1f
                    activityMainBinding.webView.alpha = alpha
                    navigationViewHeaderView.alpha = alpha
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
        }
        navHeaderMainBinding.googleEditText.setOnEditorActionListener { _, actionId, _ ->
            Log.v(TAG, "OnEditorActionListener $actionId")
            if (actionId == 6 || actionId == 5) {
                if( navHeaderMainBinding.googleEditText.text.toString() == "crash") {
                    val x = 1 / 0
                } else {
                    activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
                    goFullScreen()
                    execGoogleSearch()
                }
            }
            false
        }
        navHeaderMainBinding.httpEditText.setOnEditorActionListener { _, actionId, _ ->
            Log.v(TAG, "OnEditorActionListener $actionId")
            if (actionId == 6 || actionId == 5) {
                if( navHeaderMainBinding.httpEditText.text.toString() == "crash") {
                    val x = 1 / 0
                } else {
                    activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
                    goFullScreen()
                    execOpenUrl()
                }
            }
            false
        }
        activityMainBinding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                if (url == homeUrl || clearHistory) {
                    view.clearHistory()
                    view.clearCache(true)
                    view.clearFormData()
                    clearHistory = false
                }
                Log.v(TAG, "loadUrl $url")
                super.onPageFinished(view, url)
                lastUrl = url
                navHeaderMainBinding.httpEditText.setText(url)
                updateDateTime()
            }
        }
        activityMainBinding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                Log.v(TAG, "onProgressChanged progress:$progress")
                if (activityMainBinding.progressBar.visibility == View.GONE && lastUrl!!.indexOf("showphoto") < 0) {
                    activityMainBinding.progressBar.visibility = View.VISIBLE
                }
                activityMainBinding.progressBar.progress = progress
                if (progress == 100) {
                    activityMainBinding.progressBar.visibility = View.GONE
                }
            }
        }
        navHeaderMainBinding.btnBack.setOnClickListener {
            activityMainBinding.webView.goBack()
            activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        navHeaderMainBinding.btnSettings.setOnClickListener {
            activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
            goFullScreen()
            homeUrlEditText = EditText(activityMainBinding.webView.context)
            homeUrlEditText?.setText(homeUrl)
            homeUrlEditText?.inputType = 144
            val builder = AlertDialog.Builder(activityMainBinding.webView.context)
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
        navHeaderMainBinding.btnRefresh.setOnClickListener {
            activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
            goFullScreen()
            activityMainBinding.webView.reload()
        }
        navHeaderMainBinding.btnCancel.setOnClickListener {
            activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
            goFullScreen()
            activityMainBinding.webView.stopLoading()
        }
        navHeaderMainBinding.btnHome.setOnClickListener {
            activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
            goFullScreen()
            homeUrl?.let { it1 -> activityMainBinding.webView.loadUrl(it1) }
            clearHistory = true
        }
        navHeaderMainBinding.btnExit.setOnClickListener {
            activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
            finish()
        }
        navHeaderMainBinding.btnLight.setOnClickListener {
            when {
                navHeaderMainBinding.seekBarLight.progress == 0 -> navHeaderMainBinding.seekBarLight.progress = 100
                navHeaderMainBinding.seekBarLight.progress == 100 -> navHeaderMainBinding.seekBarLight.progress = 0
                navHeaderMainBinding.seekBarLight.progress < 50 -> navHeaderMainBinding.seekBarLight.progress = 0
                else -> navHeaderMainBinding.seekBarLight.progress = 100
            }
        }
        navHeaderMainBinding.titleText.text = "BLBrowser ${BuildConfig.VERSION_NAME}"
        goFullScreen()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        activityMainBinding.webView.clearCache(true)
    }

    public override fun onStart() {
        super.onStart()
        startRepeatingTask()
        updateDateTime()
    }

    public override fun onStop() {
        super.onStop()
        stopRepeatingTask()
        activityMainBinding.webView.clearCache(true)
        activityMainBinding.webView.clearFormData()
    }

    private fun startRepeatingTask() {
        mDateTimeHandlerTask.run()
    }

    private fun stopRepeatingTask() {
        mDateTimeHandler.removeCallbacks(mDateTimeHandlerTask)
    }

    public override fun onPause() {
        super.onPause()
        Log.v(TAG, "onPause")
        activityMainBinding.webView.onPause()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        activityMainBinding.webView.saveState(outState)
        m_savedInstanceState = outState
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        activityMainBinding.webView.restoreState(savedInstanceState)
    }

    public override fun onResume() {
        super.onResume()
        Log.v(TAG, "onResume")
        updateDateTime()
        goFullScreen()
        if (m_savedInstanceState == null) {
            lastUrl?.let { activityMainBinding.webView.loadUrl(it) }
        }
        activityMainBinding.webView.onResume()
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.systemBars())
            window.setDecorFitsSystemWindows(false)
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }

        val currentFocusView = currentFocus
        if (currentFocusView != null) {
            currentFocusView.clearFocus()
            currentFocusView.requestFocus()
            Log.v(TAG, "goFullScreen : focus back to currentFocus")
            return
        }
        window.decorView.clearFocus()
        window.decorView.requestFocus()
        Log.v(TAG, "goFullScreen : no view has focus")
    }

    private fun execGoogleSearch() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        activityMainBinding.webView.loadUrl("https://google.fr/search?q=" + navHeaderMainBinding.googleEditText.text)
        navHeaderMainBinding.googleEditText.setText("")
    }

    private fun execOpenUrl() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        val url = navHeaderMainBinding.httpEditText.text.replace("http://".toRegex(), "").replace("https://".toRegex(), "")
        activityMainBinding.webView.loadUrl("https://$url")
    }

    override fun onBackPressed() {
        if (activityMainBinding.webView.url != homeUrl) {
            activityMainBinding.webView.goBack()
            activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return true
    }

    companion object {
        const val TAG = "MainActivity"

        private const val INTERVAL = 60000L
        private const val DELAY_HIDE = 300L
        private const val PREFS_HOME = "HOME"
        private const val MY_PREFS_NAME = "BLBrowserPreferences"
        private const val DATE_FORMAT = "E dd       HH:mm"

        var clearHistory = false
        var homeUrl: String? = "https://google.fr"
        var lastUrl: String? = null
        private var m_savedInstanceState: Bundle? = null
    }
}
