/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-16.
 */

package pl.szczodrzynski.edziennik.ui.dialogs

import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.base.dialog.BaseDialog

class ErrorDetailsDialog(
    activity: AppCompatActivity,
    private val errors: List<ApiError>,
    private val titleRes: Int = R.string.dialog_error_details_title,
) : BaseDialog<Any>(activity) {

    override fun getTitleRes() = titleRes
    override fun getMessage() = errors.map {
        listOf(
            it.getStringReason(activity)
                .asBoldSpannable()
                .asColoredSpannable(R.attr.colorOnBackground.resolveAttr(activity)),
            activity.getString(R.string.error_unknown_format, it.errorCode, it.tag),
            if (App.devMode)
                it.throwable?.stackTraceString ?: it.throwable?.localizedMessage
            else
                it.throwable?.localizedMessage
        ).concat("\n")
    }.concat("\n\n")

    override fun isCancelable() = false
    override fun getPositiveButtonText() = R.string.close

    override suspend fun onBeforeShow(): Boolean = errors.isNotEmpty()
}
