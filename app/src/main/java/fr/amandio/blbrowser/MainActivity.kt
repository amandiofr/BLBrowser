package fr.amandio.blbrowser

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.*
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import fr.amandio.blbrowser.databinding.ActivityMainBinding
import fr.amandio.blbrowser.databinding.NavHeaderMainBinding
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var firstTimeUpdateSucceeded = true
    private var homeUrlEditText: EditText? = null

    private lateinit var activityMainBinding: ActivityMainBinding
    private lateinit var navHeaderMainBinding: NavHeaderMainBinding

    private val classesToRemove = arrayOf( "meta-bar clearfix", "meta-infos clearfix", "mb-4", "md:flex md:justify-between my-4 w-full flex-wrap", "flex justify-between items-end flex-wrap")
    private val urlsToIgnore = arrayOf(
        "pagead",
        "presage",
        "adscores",
        "bizdev",
        "exelator.com/",
        "beacon.krxd.net/",
        "lmd?",
        "outbrain",
        "ayads",
        "smartadserver",
        "facebook.com/tr",
        "adservice",
        "adsystem",
        "sentry.io",
        "securepubads",
        "recaptcha",
        "facebook.net/signals",
        "doubleclick",
        "encrypted",
        "www.gstatic.com",
        "wlemonde.fr/lmd",
        "www.google.fr/complete/search",
        "adsafeprotected",
        "encrypted-tbn0.gstatic.com",
        "keywee",
        "adsrvr",
        "adnxs",
        "seedtag",
        "ggpht",
        "youtubei",
        "www-embed-player",
        "gumgum",
        "sascdn",
        "youtube.com/embed",
        "quantcast.mgr",
        "quantserve.com",
        "quantcount.com",
        "googletagmanager",
        "justpremium",
        "360yield",
        "elasticad.net/",
        "googleapis.com/",
        "cloudfront.net/",
        "addthis",
        "/instream/",
        "beop.io",
        "r66net.com",
        ".ads.",
        ".richaudience.",
        "google.fr/gen_204",
        "static.com/og",
        "pc=SEARCH_HOMEPAGE&isMobile=true",
        "sskzlabs",
        "site-azp",
        "abc-static",
        "cdn.amplitude.com",
        "twitter.com/",
        "pbstck.com/",
        "weborama.fr/",
        "cookieless-data.com/",
        "targetemsecure.blob.core.windows.net/",
        "cdn.jsdelivr.net",
        "advert",
        "vidazoo",
        "spotxchange",
        "ews.google.com/swg",
        "polyfill.io",
        "connect.facebook.net",
        "secure.lemonde.fr",
        "scribewithgoogle",
        "play.google.com/log",
        "wrapper.lemde.fr",
        "lemonde.fr/bucket",
        "spotify.com",
        "moatpixel.com",
        "pubId",
        "moatads.com",
        "2mdn.net",
        "gdpr",
        "storage.googleapis.com",
        "forecast.lemonde.fr",
        "via.batch.com",
        "gscontxt.net",
        "indexww.com",
        "omnitagjs.com",
        "rlcdn.com",
        "rx.io",
        "pubmatic",
        "onaudience",
        "VideoAdContent",
        "criteo",
        "googletagservice",
        "flashtalking",
        "ampproject.org",
        "prebid-server.rubiconproject.com",
        "ads.stickyadstv.com",
        "casalemedia.com",
        "aniview.com",
        "technoratimedia.com",
        "lemonde.fr/js/lemonde.min.js",
        "prebid",
        "rubiconproject",
        "amazonaws",
        "ads.tv",
        "google-analytics",
        "google.com/ads",
        "googlesyndication",
        "voteplus",
        "voteminus",
        "bookmark"
    )

    var mDateTimeHandler = Handler(Looper.getMainLooper())
    private var mDateTimeHandlerTask: Runnable = object : Runnable {
        override fun run() {
            updateTopBar()
            mDateTimeHandler.postDelayed(this, INTERVAL)
        }
    }
    private val mHideHandler = Handler(Looper.getMainLooper())
    private val mHideRunnable = Runnable {
        activityMainBinding.webView.settings.javaScriptEnabled = true
        activityMainBinding.webView.settings.domStorageEnabled = true
        activityMainBinding.webView.settings.builtInZoomControls = true
        activityMainBinding.webView.settings.displayZoomControls = false
        activityMainBinding.webView.settings.loadWithOverviewMode = true
        activityMainBinding.webView.settings.useWideViewPort = true
        lastUrl?.let { activityMainBinding.webView.loadUrl(it) }
    }

    fun updateTopBar() {
        updateDateTime()
        updateBatteryState()
    }

    private fun updateDateTime() {
        navHeaderMainBinding.clockText.text = SimpleDateFormat(DATE_FORMAT, Locale.FRANCE).format(Date())
        if (firstTimeUpdateSucceeded) {
            firstTimeUpdateSucceeded = false
        }
    }

    private fun updateBatteryState() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { intentFilter ->
            applicationContext.registerReceiver(null, intentFilter)
        }
        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }
        navHeaderMainBinding.batteryText.text = "${batteryPct?.toInt().toString()}%"
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
        activityMainBinding.webView.setBackgroundColor(Color.TRANSPARENT)
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
                    @Suppress("DIVISION_BY_ZERO", "UNUSED_VARIABLE") val x = 1 / 0
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
                    @Suppress("DIVISION_BY_ZERO", "UNUSED_VARIABLE") val x = 1 / 0
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
                // view.loadUrl("javascript:(function() { document.getElementsByClassName('meta-bar clearfix')[0].style.display='none';}) ()")
                for( classToRemove in classesToRemove) {
                    view.loadUrl("javascript:(function() { Array.from(document.getElementsByClassName('$classToRemove')).forEach(function(element) {element.style.display = 'none';});}) ()")
                }
                lastUrl = url
                navHeaderMainBinding.httpEditText.setText(url)
                updateTopBar()
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val response = super.shouldInterceptRequest(view, request)
                Log.v(TAG, "shouldInterceptRequest response:${request?.url}")
                return response
            }

            override fun onLoadResource(view: WebView?, url: String?) {
                if( !ignoreUrl(url)) {
                    Log.v(TAG, "onLoadResource url:$url")
                    super.onLoadResource(view, url)
                } else {
                    Log.v(TAG, "onLoadResource blocked url:$url")
                }
            }
            private fun ignoreUrl(url : String?) : Boolean {
                if( url == null) return true
                for( urlToIgnore in urlsToIgnore) {
                    if( urlToIgnore in url) return true
                }
                return false
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

            @SuppressLint("StaticFieldLeak")
            private var mCustomView: View? = null
            private var mCustomViewCallback: CustomViewCallback? = null
            private var mOriginalOrientation = 0
            private var mOriginalSystemUiVisibility = 0

            override fun getDefaultVideoPoster(): Bitmap? {
                return BitmapFactory.decodeResource(applicationContext.resources, 2130837573)
            }

            override fun onHideCustomView() {
                (window.decorView as FrameLayout).removeView(mCustomView)
                mCustomView = null
                window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
                requestedOrientation = mOriginalOrientation
                mCustomViewCallback!!.onCustomViewHidden()
                mCustomViewCallback = null
            }

            override fun onShowCustomView(paramView: View?, paramCustomViewCallback: CustomViewCallback?) {
                if (mCustomView != null) {
                    onHideCustomView()
                    return
                }
                mCustomView = paramView
                mOriginalSystemUiVisibility = window.decorView.systemUiVisibility
                mOriginalOrientation = requestedOrientation
                mCustomViewCallback = paramCustomViewCallback
                (window.decorView as FrameLayout).addView(mCustomView, FrameLayout.LayoutParams(-1, -1))
                window.decorView.systemUiVisibility = 3846
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
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
                    goHome()
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
            goHome()
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

    @Synchronized
    private fun checkAppUpdate() {
        Log.v(TAG, "checkAppUpdate")
        val appUpdateManager = AppUpdateManagerFactory.create(activityMainBinding.webView.context)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            Log.v(TAG, "checkAppUpdate onSuccessListener updateAvailability:${updateAvailabilityString(appUpdateInfo.updateAvailability())} installStatus:${appUpdateInfo.installStatus()}")
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                Log.v(TAG, "checkAppUpdate AppUpdateType.IMMEDIATE")
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, appUpdateRequestCode)
            }
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                Log.v(TAG, "checkAppUpdate UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS")
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, appUpdateRequestCode)
            }
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                Log.v(TAG, "checkAppUpdate InstallStatus.DOWNLOADED")
                appUpdateManager.completeUpdate()
            }
        }
    }

    private fun updateAvailabilityString( availability: Int) : String {
        return when( availability) {
            UpdateAvailability.UPDATE_AVAILABLE -> "available"
            UpdateAvailability.UPDATE_NOT_AVAILABLE -> "not available"
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> "in progress"
            UpdateAvailability.UNKNOWN -> "unknown"
            else -> "undefined"
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        activityMainBinding.webView.clearCache(true)
    }

    public override fun onStart() {
        super.onStart()
        startRepeatingTask()
        updateTopBar()
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
        updateTopBar()
        goFullScreen()
        if (m_savedInstanceState == null) {
            lastUrl?.let { activityMainBinding.webView.loadUrl(it) }
        }
        checkAppUpdate()
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

    private fun goHome() {
        homeUrl?.let { it1 -> activityMainBinding.webView.loadUrl(it1) }
        clearHistory = true
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

        private const val appUpdateRequestCode = 15

        var clearHistory = false
        var homeUrl: String? = "https://google.fr"
        var lastUrl: String? = null
        private var m_savedInstanceState: Bundle? = null
    }
}
