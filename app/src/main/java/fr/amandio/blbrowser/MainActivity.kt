package fr.amandio.blbrowser

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
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
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import fr.amandio.blbrowser.databinding.ActivityMainBinding
import fr.amandio.blbrowser.databinding.NavHeaderMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var firstTimeUpdateSucceeded = true
    private var homeUrlEditText: EditText? = null

    private lateinit var activityMainBinding: ActivityMainBinding
    private lateinit var navHeaderMainBinding: NavHeaderMainBinding

    var downloadId: Long = 0
    lateinit var downloadManager: DownloadManager

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

        Timber.plant(DebugTree())
        Timber.v("onCreate")

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
            Timber.v("OnEditorActionListener $actionId")
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
            Timber.v("OnEditorActionListener $actionId")
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
                Timber.v("loadUrl $url")
                super.onPageFinished(view, url)
                // view.loadUrl("javascript:(function() { document.getElementsByClassName('meta-bar clearfix')[0].style.display='none';}) ()")
                for( classToRemove in classesToRemove) {
                    view.loadUrl("javascript:(function() { Array.from(document.getElementsByClassName('$classToRemove')).forEach(function(element) {element.style.display = 'none';});}) ()")
                }
                lastUrl = url
                navHeaderMainBinding.httpEditText.setText(url)
                updateTopBar()
            }

            override fun onLoadResource(view: WebView?, url: String?) {
                if( !ignoreUrl(url)) {
                    super.onLoadResource(view, url)
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
        activityMainBinding.webView.setDownloadListener(DownloadListener { url, _, _, _, _ ->
            GlobalScope.launch { downloadFile( url) }
        })
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
        onBackPressedDispatcher.addCallback(this /* lifecycle owner */, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBack()
            }
        })
        navHeaderMainBinding.titleText.text = "BLBrowser ${BuildConfig.VERSION_NAME}"
        downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        goFullScreen()
    }

    private fun openFile(file: String) {
        Timber.i("openFile $file")
        try {
            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(File(file)), "image/jpeg"))
        } catch (e: Exception) {
            Timber.e("$e")
        }
    }

    @Synchronized
    private fun checkAppUpdate() {
        Timber.v("checkAppUpdate")
        val appUpdateManager = AppUpdateManagerFactory.create(activityMainBinding.webView.context)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            Timber.v("checkAppUpdate onSuccessListener updateAvailability:${updateAvailabilityString(appUpdateInfo.updateAvailability())} installStatus:${appUpdateInfo.installStatus()}")
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                Timber.v("checkAppUpdate AppUpdateType.IMMEDIATE")
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, appUpdateRequestCode)
            }
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                Timber.v("checkAppUpdate UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS")
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, appUpdateRequestCode)
            }
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                Timber.v("checkAppUpdate InstallStatus.DOWNLOADED")
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

    private fun downloadFile( url : String?) {
        var missingPermissions = missingExternalStoragePermissions()
        if(missingPermissions.isNotEmpty()) {
            requestExternalStoragePermission(missingPermissions)
            missingPermissions = missingExternalStoragePermissions()
        }
        if(missingPermissions.isEmpty()) {
            val uri = Uri.parse(url)
            val request = DownloadManager.Request(uri)
            val fileName = uri.lastPathSegment
            Toast.makeText(applicationContext, "Download $fileName", Toast.LENGTH_SHORT).show()
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            downloadId = downloadManager.enqueue(request)?:0

            var isDownloadFinished = false
            var loopCount = 0
            while (!isDownloadFinished && loopCount <50 ) {
                loopCount ++
                Timber.v("checking download $loopCount")
                try {
                    downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
                        if (cursor.moveToFirst()) {
                                val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                                when (cursor.getInt(columnIndex)) {
                                    DownloadManager.STATUS_FAILED -> {
                                        val reasonColumn = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                        var reason = " reason : $reasonColumn "
                                        reason += when (reasonColumn) {
                                            DownloadManager.ERROR_CANNOT_RESUME -> "ERROR_CANNOT_RESUME"
                                            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "ERROR_DEVICE_NOT_FOUND"
                                            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "ERROR_FILE_ALREADY_EXISTS"
                                            DownloadManager.ERROR_FILE_ERROR -> "ERROR_FILE_ERROR"
                                            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "ERROR_INSUFFICIENT_SPACE"
                                            DownloadManager.ERROR_HTTP_DATA_ERROR -> "ERROR_HTTP_DATA_ERROR"
                                            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "ERROR_TOO_MANY_REDIRECTS"
                                            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "ERROR_UNHANDLED_HTTP_CODE"
                                            DownloadManager.ERROR_UNKNOWN -> "ERROR_UNKNOWN"
                                            else -> "ERROR_UNKNOWN"
                                        }
                                        Timber.e("failed to download file ($reason) $fileName")
                                        isDownloadFinished = true
                                    }
                                    DownloadManager.STATUS_RUNNING -> {
                                        val sizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                                        val total = cursor.getLong(sizeIndex)
                                        if (total >= 0) {
                                            val downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                            val downloaded = cursor.getLong(downloadedIndex)
                                        }
                                    }
                                    DownloadManager.STATUS_SUCCESSFUL -> {
                                        isDownloadFinished = true

                                        try {
                                            Thread.sleep(1000)
                                        } catch (e: InterruptedException) {
                                            Timber.e("This thread was interrupted")
                                        }

                                        val intent = Intent(Intent.ACTION_VIEW)
                                        intent.setDataAndType(uri, "image/jpeg")
                                        intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION

                                        val infos: List<ResolveInfo> = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                                        if (infos.isNotEmpty()) {
                                            for (info in infos) grantUriPermission(info.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            startActivity(intent)
                                        } else {
                                            Toast.makeText(applicationContext, "Can't open file $fileName", Toast.LENGTH_SHORT).show()
                                        }

                                    }
                                }
                        } else {
                            isDownloadFinished = true
                            Timber.e( "failed to download file $fileName")
                        }
                        try {
                            Thread.sleep(200)
                        } catch (e: InterruptedException) {
                            Timber.e("This thread was interrupted")
                        }
                    }
                } catch (exception: Exception) {
                    Timber.e("failed to download file $fileName")
                }
            }
            if( !isDownloadFinished) {
                Toast.makeText(applicationContext, "Can't open file $fileName", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(applicationContext, "Missing permissions ${missingPermissions.contentToString()}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestExternalStoragePermission(requestPermissions : Array<String>) {
        ActivityCompat.requestPermissions(this, requestPermissions,0)
    }

    private fun missingExternalStoragePermissions() : Array<String> {
        val requestPermissions: MutableList<String> = ArrayList()
        if (Build.VERSION.SDK_INT >= 33) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_DENIED) requestPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_DENIED) requestPermissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_DENIED) requestPermissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        }
        if (Build.VERSION.SDK_INT < 29) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) requestPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) requestPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        return requestPermissions.toTypedArray()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        activityMainBinding.webView.clearCache(true)
    }

    public override fun onStart() {
        super.onStart()
        startRepeatingTask()
        updateTopBar()
        goFullScreen()
        checkAppUpdate()
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
        Timber.v("onPause")
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
        Timber.v("onResume")
        updateTopBar()
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
        Timber.v("goFullScreen")

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
            Timber.v("goFullScreen : focus back to currentFocus")
            return
        }
        window.decorView.clearFocus()
        window.decorView.requestFocus()
        Timber.v("goFullScreen : no view has focus")
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

    private fun goBack() {
        if (activityMainBinding.webView.url != homeUrl) {
            activityMainBinding.webView.goBack()
            activityMainBinding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return true
    }

    companion object {
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
