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
import android.webkit.*
import android.widget.EditText
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
                return ( url == null ||
                        "pagead" in url ||
                        "presage" in url ||
                        "adscores" in url ||
                        "bizdev" in url ||
                        "exelator.com/" in url ||
                        "beacon.krxd.net/" in url ||
                        "lmd?" in url ||
                        "outbrain" in url ||
                        "ayads" in url ||
                        "smartadserver" in url ||
                        "facebook.com/tr" in url ||
                        "adservice" in url ||
                        "adsystem" in url ||
                        "sentry.io" in url ||
                        "securepubads" in url ||
                        "recaptcha" in url ||
                        "facebook.net/signals" in url ||
                        "doubleclick" in url ||
                        "encrypted" in url ||
                        "www.gstatic.com" in url ||
                        "wlemonde.fr/lmd" in url ||
                        "www.google.fr/complete/search" in url ||
                        "adsafeprotected" in url ||
                        "encrypted-tbn0.gstatic.com" in url ||
                        "keywee" in url ||
                        "adsrvr" in url ||
                        "adnxs" in url ||
                        "seedtag" in url ||
                        "ggpht" in url ||
                        "youtubei" in url ||
                        "www-embed-player" in url ||
                        "gumgum" in url ||
                        "sascdn" in url ||
                        "youtube.com/embed" in url ||
                        "quantcast.mgr" in url ||
                        "quantserve.com" in url ||
                        "quantcount.com" in url ||
                        "googletagmanager" in url ||
                        "justpremium" in url ||
                        "360yield" in url ||
                        "elasticad.net/" in url ||
                        "googleapis.com/" in url ||
                        "cloudfront.net/" in url ||
                        "addthis" in url ||
                        "/instream/" in url ||
                        "beop.io" in url ||
                        "r66net.com" in url ||
                        ".ads." in url ||
                        ".richaudience." in url ||
                        "google.fr/gen_204" in url ||
                        "static.com/og" in url ||
                        "pc=SEARCH_HOMEPAGE&isMobile=true" in url ||
                        "sskzlabs" in url ||
                        "site-azp" in url ||
                        "abc-static" in url ||
                        "cdn.amplitude.com" in url ||
                        "twitter.com/" in url ||
                        "pbstck.com/" in url ||
                        "weborama.fr/" in url ||
                        "cookieless-data.com/" in url ||
                        "targetemsecure.blob.core.windows.net/" in url ||
                        "cdn.jsdelivr.net" in url ||
                        "advert" in url ||
                        "vidazoo" in url ||
                        "spotxchange" in url ||
                        "ews.google.com/swg" in url ||
                        "polyfill.io" in url ||
                        "connect.facebook.net" in url ||
                        "secure.lemonde.fr" in url ||
                        "scribewithgoogle" in url ||
                        "play.google.com/log" in url ||
                        "wrapper.lemde.fr" in url ||
                        "lemonde.fr/bucket" in url ||
                        "spotify.com" in url ||
                        "pubId" in url ||
                        "2mdn.net" in url ||
                        "gdpr" in url ||
                        "storage.googleapis.com" in url ||
                        "forecast.lemonde.fr" in url ||
                        "via.batch.com" in url ||
                        "gscontxt.net" in url ||
                        "indexww.com" in url ||
                        "omnitagjs.com" in url ||
                        "rlcdn.com" in url ||
                        "rx.io" in url ||
                        "pubmatic" in url ||
                        "onaudience" in url ||
                        "VideoAdContent" in url ||
                        "criteo" in url ||
                        "googletagservice" in url ||
                        "flashtalking" in url ||
                        "ampproject.org" in url ||
                        "prebid-server.rubiconproject.com" in url ||
                        "ads.stickyadstv.com" in url ||
                        "casalemedia.com" in url ||
                        "aniview.com" in url ||
                        "technoratimedia.com" in url ||
                        "lemonde.fr/js/lemonde.min.js" in url ||
                        "prebid" in url ||
                        "rubiconproject" in url ||
                        "amazonaws" in url ||
                        "ads.tv" in url ||
                        "google-analytics" in url ||
                        "google.com/ads" in url ||
                        "googlesyndication" in url
                        )
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
        checkAppUpdate()
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
