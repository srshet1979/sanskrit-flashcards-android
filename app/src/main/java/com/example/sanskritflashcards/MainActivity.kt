package com.example.sanskritflashcards // Your package name

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.ValueCallback
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.io.IOException
import android.webkit.ConsoleMessage

class MainActivity : AppCompatActivity() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_REQUEST_CODE = 1

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myWebView: WebView = findViewById(R.id.webView)
        val webSettings = myWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true

        myWebView.addJavascriptInterface(WebAppInterface(), "AndroidInterface")

        myWebView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback = filePathCallback
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                startActivityForResult(Intent.createChooser(intent, "Select File"), FILE_CHOOSER_REQUEST_CODE)
                return true
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d(
                        "WebViewConsole", // Tag for filtering JavaScript console messages
                        "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}"
                    )
                }
                return true
            }
        }

        if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        myWebView.loadUrl("file:///android_asset/index.html")
    }

    inner class WebAppInterface {
        @android.webkit.JavascriptInterface
        fun getSanskritWords(): String {
            Log.d("MainActivity", "WebAppInterface.getSanskritWords() called by JavaScript.")
            val wordsData = loadWordsFromRawResource()
            Log.d("MainActivity", "WebAppInterface.getSanskritWords() returning data of length: ${wordsData.length}")
            return wordsData
        }

        @android.webkit.JavascriptInterface
        fun performHapticFeedback() {
            findViewById<WebView>(R.id.webView).performHapticFeedback(
                android.view.HapticFeedbackConstants.LONG_PRESS
            )
        }
    }

    private fun loadWordsFromRawResource(): String {
        Log.d("MainActivity", "loadWordsFromRawResource: Starting to read from R.raw.sanskrit_words.")
        val stringBuilder = StringBuilder()
        var linesEncountered = 0
        var linesAppended = 0
        try {
            resources.openRawResource(R.raw.sanskrit_words).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    linesEncountered++
                    // Log only the first few lines to avoid flooding Logcat if the file is large
                    if (linesEncountered <= 5) {
                        Log.d("MainActivityRawRead", "Line $linesEncountered raw content: '$line'")
                    } else if (linesEncountered == 6) {
                        Log.d("MainActivityRawRead", "Line $linesEncountered: ... (further raw line logging suppressed)")
                    }

                    if (line.isNotBlank() && !line.startsWith("#")) {
                        stringBuilder.append(line).append("\\n") // Append JS-compatible newline
                        linesAppended++
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("MainActivity", "loadWordsFromRawResource: Error loading words from raw resource", e)
            return ""
        }
        // Remove the last newline character if it exists
        if (stringBuilder.isNotEmpty() && stringBuilder.endsWith("\\n")) {
            stringBuilder.setLength(stringBuilder.length - 2)
        }

        Log.d("MainActivity", "loadWordsFromRawResource: Total lines encountered in file: $linesEncountered")
        Log.d("MainActivity", "loadWordsFromRawResource: Lines appended to stringBuilder (non-empty, non-comment): $linesAppended")
        Log.d("MainActivity", "loadWordsFromRawResource: Final stringBuilder.length: ${stringBuilder.length}")
        if (stringBuilder.isNotEmpty() && stringBuilder.length < 500) { // Log small full strings for verification
            Log.d("MainActivity", "loadWordsFromRawResource: Full stringBuilder content: $stringBuilder")
        } else if (stringBuilder.isNotEmpty()) {
            Log.d("MainActivity", "loadWordsFromRawResource: stringBuilder snippet (first 200): ${stringBuilder.substring(0, Math.min(200, stringBuilder.length))}...")
        }

        return stringBuilder.toString()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            filePathCallback?.onReceiveValue(
                if (resultCode == RESULT_OK) WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                else null
            )
            filePathCallback = null
        }
    }
}
