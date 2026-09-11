/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-7.
 */

package pl.szczodrzynski.edziennik.data.api.task

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.db.entity.Notification
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import timber.log.Timber

/**
 * Finalizes a local e-journal synchronization without contacting an app backend.
 */
class LocalPostSyncTask(
    private val app: App,
    @Suppress("UNUSED_PARAMETER") syncingProfiles: List<Profile>,
) : IApiTask(-1) {
    private val profiles by lazy { app.db.profileDao().allNow }
    private val notificationList = mutableListOf<Notification>()

    override fun prepare(app: App) {
        taskName = app.getString(R.string.edziennik_szkolny_creating_notifications)
    }

    override fun cancel() = Unit

    internal fun run(taskCallback: EdziennikCallback) {
        val startTime = System.currentTimeMillis()

        Notifications(app, notificationList, profiles).run()
        Timber.d("Created ${notificationList.count()} local notifications.")

        notificationList
            .mapNotNull { it.profileId }
            .distinct()
            .map { app.config[it].sync.notificationFilter }
            .forEach { filter ->
                filter.forEach { type ->
                    notificationList.removeAll { it.type == type }
                }
            }

        app.db.metadataDao().setAllNotified(true)
        if (notificationList.isNotEmpty()) {
            app.db.notificationDao().addAll(notificationList)
        }
        app.db.profileDao().setAllNotEmpty()

        PostNotifications(app, notificationList)
        Timber.d("LocalPostSyncTask finished in ${System.currentTimeMillis() - startTime} ms.")
        taskCallback.onCompleted()
    }
}
