package moe.wsl.lab.cleartrackparams

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import moe.wsl.lab.cleartrackparams.utils.ClearUrlUtils
import moe.wsl.lab.cleartrackparams.utils.IClearCompletion
import moe.wsl.lab.cleartrackparams.utils.data.ClearAction
import moe.wsl.lab.cleartrackparams.utils.data.ConfigFile
import moe.wsl.lab.cleartrackparams.utils.data.JavascriptExecuteException
import moe.wsl.lab.cleartrackparams.utils.data.UrlItem
import java.io.BufferedReader
import java.io.InputStreamReader

class ShareReceiveActivity : AppCompatActivity() {
    private lateinit var configFile: ConfigFile

    private lateinit var clearUrlUtils: ClearUrlUtils
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_receive)


        when (intent?.action) {
            Intent.ACTION_SEND -> {
                initTools()
                handleSend(intent)
            }
            GlobalValues.INTENT_ACTION_CLEAN_WITH_CALLBACK -> {
                initTools()
                cleanWithCallback(intent)
            }
        }
    }

    private fun initTools() {
        webView = findViewById<WebView>(R.id.controlWebView)
        configFile = readAssetsConfigFile()
        clearUrlUtils = ClearUrlUtils(
            configFile,
            object : IClearCompletion {
                override fun completion(content: String) {
                    handleCompletion(content)
                }

                override fun failed(e: Throwable) {
                    e.printStackTrace()
                    e as JavascriptExecuteException
                    Toast.makeText(baseContext, R.string.text_script_execute_failed, Toast.LENGTH_SHORT).show()
                    Log.d("JSERR", e.javascriptErrorMessage.toString())
                }
            },
            this,
            webView)
    }

    private fun cleanWithCallback(intent: Intent) {
        val content = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        clearUrlUtils.cleanUrlsInText(content)
    }

    private fun handleSend(intent: Intent) {
        val content = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        clearUrlUtils.cleanUrlsInText(content)
    }

    private fun handleCompletion(content: String) {
        val mimeType: String = if (intent.type == null) "text/plain" else intent.type!!
        Log.d("Mimetype", mimeType)

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                makeShare(content, mimeType)
            }
            GlobalValues.INTENT_ACTION_CLEAN_WITH_CALLBACK -> {
                val resultCode = intent.getIntExtra("ResultCode", 0)
                intent.putExtra(Intent.EXTRA_TEXT, content)
                setResult(resultCode, intent)
                this.finish()
            }
        }
    }

    private fun makeShare(url: String, mimeType: String) {
        val title = if (url.length > 32) {
            url.substring(0, 32) + "..."
        } else {
            url
        }
        val share = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            type = mimeType
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(Intent.EXTRA_TEXT, url)
        }, title)
        startActivityForResult(share, 1)
    }

    private fun readAssetsConfigFile(): ConfigFile {
        val fileContent = StringBuilder()
        val fileStream = assets.open("embed_rules.toml")
        val bufferedReader = BufferedReader(InputStreamReader(fileStream))
        var line: String? = bufferedReader.readLine()
        while (line != null) {
            fileContent.append(line + '\n')
            line = bufferedReader.readLine()
        }
        return ConfigFile.readFromTomlText(fileContent.toString())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        this.finish()
    }
}