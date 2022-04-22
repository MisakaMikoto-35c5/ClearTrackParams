package moe.wsl.lab.cleartrackparams

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CleanFromClipboardActivity : AppCompatActivity() {
    private var clipboard: ClipboardManager? = null
    private lateinit var resultTextEdit: EditText
    private lateinit var pasteAndCleanButton: Button
    private lateinit var copyButton: Button
    private lateinit var shareButton: Button

    private var cleanResult = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clean_from_clipboard)

        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        resultTextEdit = findViewById(R.id.resultTextEdit)
        pasteAndCleanButton = findViewById(R.id.pasteAndCleanButton)
        copyButton = findViewById(R.id.copyButton)
        shareButton = findViewById(R.id.shareButton)

        copyButton.setOnClickListener {
            clipboard!!.setPrimaryClip(ClipData.newPlainText("", cleanResult))
            Toast.makeText(this, R.string.text_copied, Toast.LENGTH_SHORT).show()
        }
        pasteAndCleanButton.setOnClickListener {
            cleanFromClipboard()
        }
        shareButton.setOnClickListener {
            val share = Intent.createChooser(Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, cleanResult)
                type = "text/plain"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }, null)
            startActivity(share)
        }

        cleanFromClipboard()
    }

    private fun cleanFromClipboard() {
        val clipboardData = clipboard?.primaryClip
        if (clipboardData == null) {
            Toast.makeText(this, R.string.text_no_data_in_clipboard, Toast.LENGTH_SHORT).show()
            return
        }

        val clipboardElem = clipboardData.getItemAt(0)
        val clipboardText = clipboardElem.text.toString()
        Log.d("test", "Clipboard data: $clipboardText")
        val intent = Intent(this, ShareReceiveActivity::class.java)
        intent.action = GlobalValues.INTENT_ACTION_CLEAN_WITH_CALLBACK
        intent.putExtra(Intent.EXTRA_TEXT, clipboardText)
        startActivityForResult(intent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return
        val result = data.getStringExtra(Intent.EXTRA_TEXT) ?: return
        cleanResult = result
        resultTextEdit.setText(result)
    }
}