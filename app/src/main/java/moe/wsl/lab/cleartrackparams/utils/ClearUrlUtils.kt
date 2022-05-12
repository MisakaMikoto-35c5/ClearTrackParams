package moe.wsl.lab.cleartrackparams.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.webkit.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.wsl.lab.cleartrackparams.BuildConfig
import moe.wsl.lab.cleartrackparams.utils.data.*
import java.lang.RuntimeException
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap


@SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
class ClearUrlUtils(
    private val config: ConfigFile,
    private val completion: IClearCompletion,
    private val context: Context,
    private val webView: WebView,
) {
    companion object {
        private const val LOG_TAG = "ClearUrlUtils"

        fun splitQuery(url: URL): Map<String, String> {
            val queryPairs: MutableMap<String, String> = LinkedHashMap()
            val query = url.query
            val pairs = query.split("&").toTypedArray()
            for (pair in pairs) {
                if (pair.isEmpty()) {
                    continue
                }
                val idx = pair.indexOf("=")
                queryPairs[pair.substring(0, idx)] = pair.substring(idx + 1)
            }
            return queryPairs
        }

        fun cleanUrlParams(url: String, keywords: Array<String>?, isBlacklist: Boolean): String {
            val jUrl = URL(url)
            val port: String = when (jUrl.protocol) {
                "http" -> if (jUrl.port == 80 || jUrl.port == -1) "" else ":" + jUrl.port
                "https" -> if (jUrl.port == 443 || jUrl.port == -1) "" else ":" + jUrl.port
                else -> ""
            }
            val urlBase = jUrl.protocol + "://" + jUrl.host + port + jUrl.path
            if (jUrl.query == null || jUrl.query.isEmpty() || keywords == null) {
                return urlBase
            }
            val queryParamsString = StringBuilder()
            val queryParams = splitQuery(jUrl)
            queryParams.forEach {
                var isExisted = false
                for (i in keywords) {
                    if (it.key == i) {
                        isExisted = true
                        break
                    }
                }
                if (isBlacklist && !isExisted || !isBlacklist && isExisted) {
                    queryParamsString.append("${it.key}=${it.value}&")
                }
            }
            if (queryParamsString.isEmpty()) {
                return urlBase
            }

            return "$urlBase?${queryParamsString.substring(0, queryParamsString.length - 1)}"
        }
    }

    private val paddingHtmlFile = "file:///android_asset/execute_script.html"
    private val urlRegex = Regex(
        "https?:\\/\\/([0-9a-zA-Z\\-_]+\\.)+([a-zA-Z]+|(xn--[0-9a-zA-Z]+))+\\/[A-Za-z0-9\\-\\?\\+&@#/%=~_\\.\\|]+"
    )
    private val controlPassword = UUID.randomUUID().toString()
    private val rules: Array<UrlItem>

    // ------ START CLEAN JOB STATE AREA ------
    private var originUrl = ""
    private var firstLoadUrl = ""
    private var allowLoadUrl = false
    private var isFirstLoad = true
    private var text = ""
    private var matches: Sequence<MatchResult> = sequence {  }
    private var currentUrlItem: UrlItem? = null
    private var currentMatchPos = 0
    private var omitChars = 0
    private var cleanedUrl: String? = null
    // ------ END CLEAN JOB STATE AREA ------

    private fun clearState() {
        originUrl = ""
        firstLoadUrl = ""
        allowLoadUrl = false
        isFirstLoad = false
        text = ""
        matches = sequence {  }
        currentUrlItem = null
        currentMatchPos = 0
        omitChars = 0
        cleanedUrl = null
        clearWebViewCache()
    }

    private val webViewClient = object : WebViewClient() {

        var isUrlStartLoad = false

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                shouldOverrideUrlLoading(webView, request.url.toString())
            } else {
                return true
            }
        }
        override fun shouldOverrideUrlLoading(view: WebView, urlNewString: String): Boolean {
            //Log.d("urlNewString", urlNewString)
            if (allowLoadUrl) {
                return false
            }
            if (urlNewString.startsWith(paddingHtmlFile)) {
                return false
            }
            originUrl = urlNewString
            val paddingUrl = paddingHtmlFile + "?newUrl=" + java.net.URLEncoder.encode(urlNewString, "utf-8")
            //Log.d("Padding url", paddingUrl)
            view.loadUrl(paddingUrl)
            return true
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            isUrlStartLoad = true
            val realUrl = url.toString()
            //Log.d("Url Loading", realUrl)
            val urlItem = currentUrlItem ?: return
            val script = when (urlItem.action) {
                ClearAction.SHORT_LINK -> {
                    "document.onUrlPreLoad = (url) => {\n" +
                    "    if (url === originalUrl) {\n" +
                    "        control.changeAllowLoadUrl();\n" +
                    "        location.href = url;\n" +
                    "        return;\n" +
                    "    }\n" +
                    "    control.completion(url);\n" +
                    "}"
                }
                else -> {
                    urlItem.javascript
                }
            }
            //Log.d("script", script.toString())
            view.evaluateJavascript(
                "(function () { var _controlObject = clearUrlUtils;" +
                        "var controlPassword = '$controlPassword';" +
                        "var originalUrl = " + Json.encodeToString(firstLoadUrl) + '\n' +
                        "var control = {" +
                        "changeAllowLoadUrl: function() {return _controlObject.changeAllowLoadUrl(controlPassword)}," +
                        "changeNextUrl: function(url) {return controlObject.changeNextUrl(controlPassword, url)}," +
                        "completion: function(result) {return _controlObject.completion(controlPassword, result)}," +
                        "error: function(message) {return _controlObject.error(controlPassword, message)}," +
                        "cleanUrlParams: function(url, keywords, isBlacklist) {return _controlObject.cleanUrlParams(url, keywords, isBlacklist)}," +
                        "getVersion: function() {return _controlObject.getVersion()}," +
                        "};" +
                        "document.clearUrlUtils = undefined;" +
                        "try {" +
                        config.rule_configuration.webview_pre_execute +
                        "} catch (e) { control.error(e.stack); }" +
                        "try {" +
                        script +
                        "} catch (e) { control.error(e.stack); }" +
                        "})()"
            ) {}
            // Log.d(LOG_TAG, "Current url: $realUrl, Origin url: $originUrl, isAllowLoadUrl: $allowLoadUrl")
            if (realUrl.startsWith(paddingHtmlFile)) {
                view.evaluateJavascript("if (document.onUrlPreLoad) {document.onUrlPreLoad('$originUrl')}") {}
            } else {
                view.evaluateJavascript("if (document.onUrlLoad) {document.onUrlLoad('$originUrl')}") {}
            }
            super.onPageStarted(view, url, favicon)
        }

        private fun generateSetInnerHtmlScript(keyword: String, content: String?): String {
            return "try { document.getElementById('$keyword').innerHTML = " +
                    Json.encodeToString(content) +
                    " } catch (e) { console.log(e); };"
        }

        override fun onPageFinished(view: WebView, url: String) {
            val urlItem = currentUrlItem ?: return
            if (url == paddingHtmlFile) {
                view.evaluateJavascript(
                    generateSetInnerHtmlScript("current_url", originUrl) +
                            generateSetInnerHtmlScript("pre_webview_script", config.rule_configuration.webview_pre_execute)+
                            generateSetInnerHtmlScript("matched_regex", urlItem.regex) +
                            generateSetInnerHtmlScript("rule_script", urlItem.javascript)
                ) {}
            } else {
                view.evaluateJavascript("if (document.onUrlLoadFinish) {document.onUrlLoadFinish('$originUrl')}") {}
            }
            super.onPageFinished(view, url)
        }
    }

    init {
        webView.settings.javaScriptEnabled = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webView.webViewClient = webViewClient
        webView.addJavascriptInterface(WebViewJavascriptInterface(this), "clearUrlUtils")
        rules = config.rules
    }

    /**
     * Find URL in rules database
     */
    private fun matchUrl(url: String): MatchedUrlItem? {
        var matchedUrlItem: UrlItem? = null
        for (i in rules) {
            if (i.pattern.matches(url)) {
                matchedUrlItem = i
                break
            }
        }
        if (matchedUrlItem != null) {
            return MatchedUrlItem(url, matchedUrlItem)
        }
        return null
    }

    /**
     * Clean a URL
     */
    private fun cleanUrl(url: String) {
        val matchedUrl = matchUrl(url)
        if (matchedUrl == null) {
            cleanedUrl = url
            cleanUrlsInText()
            return
        }
        val urlItem = matchedUrl.urlItem
        currentUrlItem = urlItem
        originUrl = url
        firstLoadUrl = url
        when (urlItem.action) {
            ClearAction.CLEAR_ALL_PARAM -> {
                Log.d(LOG_TAG, "Use CLEAR_ALL_PARAM method.")
                cleanedUrl = url.split('?')[0]
                cleanUrlsInText()
            }
            ClearAction.LUA -> {
                Log.d(LOG_TAG, "Use LUA method.")
                callError(NotImplementedError("Not support lua."))
            }
            ClearAction.PARAM_BLACKLIST -> {
                Log.d(LOG_TAG, "Use PARAM_BLACKLIST method.")
                cleanedUrl = cleanUrlParams(url, urlItem.params, true)
                cleanUrlsInText()
            }
            ClearAction.PARAM_WHITELIST -> {
                Log.d(LOG_TAG, "Use PARAM_WHITELIST method.")
                cleanedUrl = cleanUrlParams(url, urlItem.params, false)
                cleanUrlsInText()
            }
            ClearAction.WEB_VIEW -> {
                Log.d(LOG_TAG, "Use WEB_VIEW method.")
                webView.loadUrl(url)
            }
            ClearAction.SHORT_LINK -> {
                Log.d(LOG_TAG, "Use SHORT_LINK method.")
                webView.loadUrl(url)
            }
            else -> callError(NotImplementedError("Unknown action ${urlItem.action}."))
        }
    }

    /**
     * Clean url in text method
     */
    fun cleanUrlsInText(text: String) {
        this.text = text
        matches = urlRegex.findAll(text)
        cleanUrlsInText()
    }

    private fun cleanCommonTrackParams(url: String): String {
        val jUrl = URL(url)
        val port: String = when (jUrl.protocol) {
            "http" -> if (jUrl.port == 80 || jUrl.port == -1) "" else ":" + jUrl.port
            "https" -> if (jUrl.port == 443 || jUrl.port == -1) "" else ":" + jUrl.port
            else -> ""
        }
        val urlBase = jUrl.protocol + "://" + jUrl.host + port + jUrl.path
        if (jUrl.query == null || jUrl.query.isEmpty()) {
            return urlBase
        }
        val queryParamsString = StringBuilder()
        val queryParams = splitQuery(jUrl)
        queryParams.forEach {
            var isExisted = false
            for (i in config.rule_configuration.common_track_args_pattern) {
                Log.d("pattern", i.toString())
                if (i.containsMatchIn(it.key)) {
                    isExisted = true
                    break
                }
            }
            if (!isExisted) {
                queryParamsString.append("${it.key}=${it.value}&")
            }
        }
        if (queryParamsString.isEmpty()) {
            return urlBase
        }
        return "$urlBase?${queryParamsString.substring(0, queryParamsString.length - 1)}"
    }

    /**
     * Internal method, will find a link in text and clean it.
     */
    private fun cleanUrlsInText() {
        if (cleanedUrl != null) {
            cleanedUrl = cleanCommonTrackParams(cleanedUrl!!)
            Log.d("CleanedUrl", cleanedUrl.toString())
        }
        if (currentUrlItem != null && currentUrlItem!!.action == ClearAction.SHORT_LINK && cleanedUrl != null) {
            val url = cleanedUrl.toString()
            val newUrlMatch = matchUrl(url)
            if (newUrlMatch != null) {
                cleanUrl(url)
                return
            }
        }
        if (cleanedUrl != null) {
            val url = cleanedUrl !!
            val match = matches.elementAt(currentMatchPos)
            text = text.substring(0, match.range.first - omitChars) +
                    url +
                    text.substring(match.range.last - omitChars + 1)
            omitChars += match.value.length - url.length + 1
            currentMatchPos += 1
            cleanedUrl = null
            if (currentMatchPos >= matches.count()) {
                callAllCompleted()
                return
            }
        }
        if (matches.count() == 0) {
            callAllCompleted()
            return
        }
        val nextMatch = matches.elementAt(currentMatchPos)
        cleanUrl(nextMatch.value)
    }

    /**
     * Completion callback
     */
    private fun callAllCompleted() {
        try {
            completion.completion(text)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        clearState()
    }

    /**
     * Error callback
     */
    private fun callError(err: Throwable) {
        Log.d(LOG_TAG, "Clean error: $err")
        try {
            completion.failed(err)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        clearState()
    }

    private fun clearWebViewCache() {
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
    }

    /**
     * WebView Javascript interface
     */
    private class WebViewJavascriptInterface(private val parent: ClearUrlUtils) {
        @JavascriptInterface
        fun loadUrl(password: String, url: String?) {
            if (password != parent.controlPassword) {
                throw SecurityException("Incorrect password. ")
            }
            parent.allowLoadUrl = true
            if (url != null) {
                parent.webView.post {
                    parent.webView.loadUrl(url)
                }
            }
        }
        @JavascriptInterface
        fun changeAllowLoadUrl(password: String) {
            if (password != parent.controlPassword) {
                throw SecurityException("Incorrect password. ")
            }
            parent.allowLoadUrl = true
        }
        @JavascriptInterface
        fun changeNextUrl(password: String, url: String?) {
            if (password != parent.controlPassword) {
                throw SecurityException("Incorrect password. ")
            }
            parent.originUrl = url.toString()
        }
        @JavascriptInterface
        fun completion(password: String, result: String?) {
            if (password != parent.controlPassword) {
                throw SecurityException("Incorrect password. ")
            }
            parent.cleanedUrl = result.toString()
            parent.webView.post {
                parent.cleanUrlsInText()
            }
        }
        @JavascriptInterface
        fun error(password: String, errorMessage: String?) {
            if (password != parent.controlPassword) {
                throw SecurityException("Incorrect password. ")
            }
            parent.webView.post {
                val exception = JavascriptExecuteException("Javascript execute error.")
                exception.javascriptErrorMessage = errorMessage
                parent.callError(exception)
            }
        }
        @JavascriptInterface
        fun cleanUrlParams(url: String, keywords: Array<String>, isBlacklist: Boolean): String {
            return ClearUrlUtils.cleanUrlParams(url, keywords, isBlacklist)
        }
        @JavascriptInterface
        fun getVersion(): Int {
            return BuildConfig.VERSION_CODE
        }
    }
}