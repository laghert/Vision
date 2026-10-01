/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-18.
 * Modifications Copyright (c) Laghert Labs 2026.
 */

package pl.szczodrzynski.edziennik.ui.dialogs

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.databinding.DialogLibrusGuideBinding
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.toDrawable

class LibrusGuideBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "LibrusGuideBottomSheet"

        fun show(activity: AppCompatActivity) {
            val existing = activity.supportFragmentManager.findFragmentByTag(TAG)
            if (existing != null) return
            LibrusGuideBottomSheet().show(activity.supportFragmentManager, TAG)
        }

        fun show(fragment: Fragment) {
            val fm = fragment.childFragmentManager
            val existing = fm.findFragmentByTag(TAG)
            if (existing != null) return
            LibrusGuideBottomSheet().show(fm, TAG)
        }
    }

    private var _binding: DialogLibrusGuideBinding? = null
    private val b get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLibrusGuideBinding.inflate(inflater, container, false)
        return b.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        b.closeButton.setImageDrawable(
            CommunityMaterial.Icon.cmd_close.toDrawable(requireContext(), sizeDp = 22)
        )
        b.closeButton.onClick { dismiss() }

        b.webView.apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        activity?.startActivity(intent)
                        return true
                    }
                    return false
                }
            }
            loadUrl("file:///android_asset/librus-guide.html")
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        val behavior = BottomSheetBehavior.from(bottomSheet)
        val displayMetrics = resources.displayMetrics
        bottomSheet.layoutParams.height = (displayMetrics.heightPixels * 0.90).toInt()
        bottomSheet.requestLayout()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
    }

    override fun onDestroyView() {
        b.webView.stopLoading()
        _binding = null
        super.onDestroyView()
    }
}
