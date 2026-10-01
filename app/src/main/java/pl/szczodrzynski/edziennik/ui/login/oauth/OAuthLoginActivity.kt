/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-15.
 */

package pl.szczodrzynski.edziennik.ui.login.oauth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.LIBRUS_USER_AGENT
import pl.szczodrzynski.edziennik.ext.dp
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ext.toDrawable
import timber.log.Timber

class OAuthLoginActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "OAuthLoginActivity"
    }

    private var isSuccessful = false
    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.oauth_dialog_title)

        val authorizeUrl = intent.getStringExtra("authorizeUrl") ?: return
        val redirectUrl = intent.getStringExtra("redirectUrl") ?: return

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setBackgroundColor(R.attr.colorSurface.resolveAttr(this@OAuthLoginActivity))
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 56.dp)
            setPadding(12.dp, 0, 16.dp, 0)
        }

        val closeButton = ImageButton(this).apply {
            setImageDrawable(CommunityMaterial.Icon.cmd_close.toDrawable(this@OAuthLoginActivity, sizeDp = 24))
            setBackgroundResource(R.attr.selectableItemBackgroundBorderless.resolveAttr(this@OAuthLoginActivity))
            layoutParams = LinearLayout.LayoutParams(40.dp, 40.dp)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            onClick { finish() }
        }
        toolbar.addView(closeButton)

        val titleView = TextView(this).apply {
            text = getString(R.string.oauth_dialog_title)
            setTextAppearance(R.attr.textAppearanceTitleMedium.resolveAttr(this@OAuthLoginActivity))
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                marginStart = 12.dp
            }
        }
        toolbar.addView(titleView)

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 3.dp)
            isIndeterminate = false
            max = 100
            progress = 0
        }

        root.addView(toolbar)
        root.addView(progressBar)

        val webViewContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f)
        }

        val wv = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                if (authorizeUrl.contains("librus")) {
                    userAgentString = LIBRUS_USER_AGENT
                }
            }
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
        webView = wv

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (!title.isNullOrBlank() && !title.startsWith("http")) {
                    titleView.text = title
                }
            }
        }

        wv.webViewClient = object : WebViewClient() {
            private fun handleUrl(url: String?): Boolean {
                if (url == null) return false
                Timber.d("OAuth navigating to: $url")
                if (url.startsWith(redirectUrl)) {
                    isSuccessful = true
                    EventBus.getDefault().post(OAuthLoginResult(
                        isError = false,
                        responseUrl = url,
                    ))
                    finish()
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                if (handleUrl(url)) return
                super.onPageStarted(view, url, favicon)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (handleUrl(url)) return true
                return super.shouldOverrideUrlLoading(view, request)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (handleUrl(url)) return true
                return super.shouldOverrideUrlLoading(view, url)
            }
        }

        webViewContainer.addView(wv)
        root.addView(webViewContainer)
        setContentView(root)

        wv.loadUrl(authorizeUrl)
    }

    override fun onDestroy() {
        super.onDestroy()
        CookieManager.getInstance().flush()
        webView?.destroy()
        if (!isSuccessful) {
            EventBus.getDefault().post(OAuthLoginResult(
                isError = false,
                responseUrl = null,
            ))
        }
    }
}
