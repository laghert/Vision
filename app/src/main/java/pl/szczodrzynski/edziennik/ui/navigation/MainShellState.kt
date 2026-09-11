package pl.szczodrzynski.edziennik.ui.navigation

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.iconics.typeface.IIcon
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.utils.models.UnreadCounter
import pl.szczodrzynski.navlib.bottomsheet.items.IBottomSheetItem

class MainShellState(initialProfile: Profile) {
    var profile by mutableStateOf(initialProfile)
    var profiles by mutableStateOf<List<Profile>>(emptyList())
    var target by mutableStateOf(NavTarget.HOME)
    var subtitle by mutableStateOf<String?>(null)
    var unreadCounters by mutableStateOf<List<UnreadCounter>>(emptyList())

    val contextActions = ContextActionsController()
    val fab = MainFabController()
    val refresh = MainRefreshController()
}

class ContextActionsController {
    val items = mutableStateListOf<IBottomSheetItem<*>>()
    var isOpen by mutableStateOf(false)
    var onCloseListener: (() -> Unit)? = null

    operator fun plusAssign(item: IBottomSheetItem<*>) = appendItem(item)

    fun appendItem(item: IBottomSheetItem<*>) {
        items += item
    }

    fun appendItems(vararg newItems: IBottomSheetItem<*>) {
        items += newItems
    }

    fun prependItems(vararg newItems: IBottomSheetItem<*>) {
        items.addAll(0, newItems.toList())
    }

    fun addItemAt(index: Int, item: IBottomSheetItem<*>) {
        items.add(index.coerceIn(0, items.size), item)
    }

    fun removeAllItems() = items.clear()

    fun removeAllStatic() {
        items.removeAll { !it.isContextual }
    }

    fun removeAllContextual() {
        items.removeAll { it.isContextual }
    }

    fun removeItemById(id: Int) {
        items.removeAll { it.id == id }
    }

    fun removeItemAt(index: Int) {
        if (index in items.indices) items.removeAt(index)
    }

    fun getItemById(id: Int, run: (IBottomSheetItem<*>?) -> Unit) =
        run(items.singleOrNull { it.id == id })

    fun getItemByIndex(index: Int, run: (IBottomSheetItem<*>?) -> Unit) =
        run(items.getOrNull(index))

    fun open() {
        isOpen = true
    }

    fun close() {
        if (isOpen) onCloseListener?.invoke()
        isOpen = false
    }

    fun toggle() {
        if (isOpen) close() else open()
    }
}

class MainFabController {
    var enabled by mutableStateOf(false)
    var extended by mutableStateOf(false)
    var text by mutableStateOf<CharSequence>("")
    var icon by mutableStateOf<IIcon?>(null)
    private var listener: View.OnClickListener? = null

    fun configure(text: CharSequence, icon: IIcon, onClick: View.OnClickListener) {
        this.text = text
        this.icon = icon
        listener = onClick
        enabled = true
        extended = true
    }

    fun reset() {
        enabled = false
        extended = false
        listener = null
    }

    fun performClick(view: View) {
        listener?.onClick(view)
    }
}

class MainRefreshController {
    private var view: SwipeRefreshLayout? = null
    private var onRefresh: (() -> Unit)? = null
    private var enabled = false
    private var refreshing = false

    var isEnabled: Boolean
        get() = enabled
        set(value) {
            enabled = value
            view?.isEnabled = value
        }

    var isRefreshing: Boolean
        get() = refreshing
        set(value) {
            refreshing = value
            view?.isRefreshing = value
        }

    internal fun bind(view: SwipeRefreshLayout, onRefresh: () -> Unit) {
        this.view = view
        this.onRefresh = onRefresh
        view.isEnabled = enabled
        view.isRefreshing = refreshing
        view.setOnRefreshListener { this.onRefresh?.invoke() }
    }

    internal fun unbind(view: SwipeRefreshLayout) {
        if (this.view === view) this.view = null
    }
}
