/*
 * Copyright 2014-2017 Eduard Ereza Martínez
 * Licensed under the Apache License, Version 2.0.
 */

package pl.szczodrzynski.edziennik.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ext.resolveStyleAttr
import pl.szczodrzynski.edziennik.utils.html.BetterHtml

class CrashActivity : AppCompatActivity() {
    private val app by lazy { application as App }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.uiManager.applyTheme(this)
        setContentView(R.layout.activity_crash)

        val config = CustomActivityOnCrash.getConfigFromIntent(intent) ?: run {
            finish()
            return
        }

        findViewById<Button>(R.id.crash_restart_btn).setOnClickListener {
            CustomActivityOnCrash.restartApplication(this, config)
        }
        findViewById<View>(R.id.crash_report_btn).visibility = View.GONE
        findViewById<Button>(R.id.crash_details_btn).setOnClickListener {
            MaterialAlertDialogBuilder(
                this,
                R.attr.materialAlertDialogMonospaceTheme.resolveStyleAttr(this),
            )
                .setTitle(R.string.crash_details)
                .setMessage(BetterHtml.fromHtml(context = null, getErrorString(intent, false)))
                .setPositiveButton(R.string.close, null)
                .setNeutralButton(R.string.copy_to_clipboard) { _, _ -> copyErrorToClipboard() }
                .show()
        }

        val manualCrash = CustomActivityOnCrash
            .getAllErrorDetailsFromIntent(this, intent)
            .contains("MANUAL CRASH")
        findViewById<View>(R.id.crash_notice).visibility = if (manualCrash) View.GONE else View.VISIBLE
        findViewById<View>(R.id.crash_feature).visibility = if (manualCrash) View.VISIBLE else View.GONE
    }

    private fun getErrorString(intent: Intent, plain: Boolean): String {
        var plainText = "Crash report:\n\n${CustomActivityOnCrash.getStackTraceFromIntent(intent)}"
        var richText = "<small>$plainText</small>"
        val currentPackage = packageName.removeSuffix(".debug")
        richText = richText.replace(currentPackage, "<font color='#4caf50'>$currentPackage</font>")
            .replace("\n", "<br>")
        plainText += "\n${Build.MANUFACTURER}\n${Build.BRAND}\n${Build.MODEL}\n${Build.DEVICE}\n"
        plainText += "${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}"
        return if (plain) plainText else richText
    }

    private fun copyErrorToClipboard() {
        val errorInformation = CustomActivityOnCrash.getAllErrorDetailsFromIntent(this, intent)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(
            ClipData.newPlainText(
                getString(R.string.customactivityoncrash_error_activity_error_details_clipboard_label),
                errorInformation,
            ),
        )
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}
