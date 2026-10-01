package pl.szczodrzynski.edziennik.ui.login.eduvulcan

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ext.dp
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ext.toDrawable
import timber.log.Timber

/**
 * In-app WebView browser flow for eduVULCAN login.
 * Solves Cloudflare/Turnstile and captures mobile registration tokens automatically.
 */
class EduVulcanLoginActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "EduVulcanLoginActivity"
        private const val DEFAULT_START_URL = "https://eduvulcan.pl/logowanie"
    }

    private var isSuccessful = false
    private var webView: WebView? = null

    inner class VisionJsBridge {
        @JavascriptInterface
        fun onCredentialsExtracted(token: String, symbol: String, pin: String) {
            runOnUiThread {
                if (isSuccessful) return@runOnUiThread
                isSuccessful = true
                Timber.d("$TAG: Successfully extracted Vulcan credentials for symbol=$symbol")
                Toast.makeText(this@EduVulcanLoginActivity, "Pomyślnie powiązano z kontem eduVULCAN!", Toast.LENGTH_SHORT).show()
                EventBus.getDefault().post(
                    EduVulcanLoginResult(
                        isError = false,
                        token = token.trim(),
                        symbol = symbol.trim(),
                        pin = pin.trim(),
                    )
                )
                finish()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setBackgroundColor(R.attr.colorSurface.resolveAttr(this@EduVulcanLoginActivity))
        }

        // Toolbar
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 56.dp)
            setPadding(12.dp, 0, 16.dp, 0)
        }

        val closeButton = ImageButton(this).apply {
            setImageDrawable(CommunityMaterial.Icon.cmd_close.toDrawable(this@EduVulcanLoginActivity, sizeDp = 24))
            setBackgroundResource(R.attr.selectableItemBackgroundBorderless.resolveAttr(this@EduVulcanLoginActivity))
            layoutParams = LinearLayout.LayoutParams(40.dp, 40.dp)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            onClick { finish() }
        }
        toolbar.addView(closeButton)

        val titleView = TextView(this).apply {
            text = "Logowanie eduVULCAN"
            setTextAppearance(R.attr.textAppearanceTitleMedium.resolveAttr(this@EduVulcanLoginActivity))
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                marginStart = 12.dp
            }
        }
        toolbar.addView(titleView)

        // Informative helper banner
        val banner = TextView(this).apply {
            text = "Zaloguj się na swoje konto eduVULCAN. Vision automatycznie wykryje Twoje konto i połączy z dziennikiem."
            setTextAppearance(R.attr.textAppearanceBodySmall.resolveAttr(this@EduVulcanLoginActivity))
            setPadding(16.dp, 6.dp, 16.dp, 6.dp)
            setBackgroundColor(R.attr.colorPrimaryContainer.resolveAttr(this@EduVulcanLoginActivity))
            setTextColor(R.attr.colorOnPrimaryContainer.resolveAttr(this@EduVulcanLoginActivity))
        }

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 3.dp)
            isIndeterminate = false
            max = 100
            progress = 0
        }

        root.addView(toolbar)
        root.addView(banner)
        root.addView(progressBar)

        val webViewContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f)
        }

        val wv = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                userAgentString = settings.userAgentString.replace("; wv", "")
            }
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            addJavascriptInterface(VisionJsBridge(), "VisionBridge")
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
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                injectTokenExtractorScript(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return false
            }
        }

        webViewContainer.addView(wv)
        root.addView(webViewContainer)
        setContentView(root)

        wv.loadUrl(DEFAULT_START_URL)
    }

    private fun injectTokenExtractorScript(wv: WebView, currentUrl: String) {
        val js = """
            (function() {
                if (window._visionInjected) return;
                window._visionInjected = true;

                // Function to extract symbol from URL if present
                function detectSymbol() {
                    var match = window.location.pathname.match(/\/([a-zA-Z0-9_\-]+)\//);
                    return match ? match[1] : '';
                }

                // Try calling UONET+ token generation endpoint directly if authenticated
                function tryFetchToken() {
                    var symbol = detectSymbol();
                    if (!symbol) return;
                    
                    var endpoint = '/' + symbol + '/RejestracjaUrzadzeniaToken.mvc/Get';
                    fetch(endpoint, { credentials: 'include' })
                        .then(function(res) { return res.json(); })
                        .then(function(json) {
                            if (json && json.data && json.data.TokenKey && json.data.PIN) {
                                window.VisionBridge.onCredentialsExtracted(
                                    json.data.TokenKey,
                                    symbol,
                                    json.data.PIN
                                );
                            }
                        })
                        .catch(function(e) {});
                }

                // Check DOM for displayed token/symbol/pin (e.g. on Mobile Access dialog)
                function scanDomForToken() {
                    var text = document.body ? document.body.innerText : '';
                    if (!text) return;

                    var tokenMatch = text.match(/(?:Token|Kod|Token dostępu|KOD)\s*[:=]?\s*([A-Z0-9]{5,12})/i);
                    var pinMatch = text.match(/(?:PIN|Kod PIN)\s*[:=]?\s*(\d{4,8})/i);
                    var symbolMatch = text.match(/(?:Symbol)\s*[:=]?\s*([a-zA-Z0-9_\-]+)/i) || [null, detectSymbol()];

                    if (tokenMatch && pinMatch && symbolMatch && symbolMatch[1]) {
                        window.VisionBridge.onCredentialsExtracted(
                            tokenMatch[1],
                            symbolMatch[1],
                            pinMatch[1]
                        );
                    }
                }

                // Periodic check
                tryFetchToken();
                scanDomForToken();
                setInterval(function() {
                    tryFetchToken();
                    scanDomForToken();
                }, 1500);
            })();
        """.trimIndent()

        wv.evaluateJavascript(js, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        CookieManager.getInstance().flush()
        webView?.destroy()
        if (!isSuccessful) {
            EventBus.getDefault().post(EduVulcanLoginResult(isError = false))
        }
    }
}
