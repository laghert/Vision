package pl.szczodrzynski.edziennik.ui.grades.editor

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.iconics.view.IconicsImageView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.utils.Colors.gradeNameToColor
import java.text.DecimalFormat

class GradesEditorAdapter(
        private val mContext: Context,
        private val gradeList: List<GradesEditorFragment.EditorGrade>,
        private val fragment: GradesEditorFragment,
        private val listener: OnGradeActionListener,
) : RecyclerView.Adapter<GradesEditorAdapter.ViewHolder>() {

    interface OnGradeActionListener {
        fun onClickRemove(gradeId: Long)
        fun onClickEdit(gradeId: Long)
        fun onClickAdd()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //inflating and returning our view holder
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.row_grades_editor_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = mContext.applicationContext as App

        val editorGrade = gradeList[position]

        holder.gradesListRoot.setOnClickListener { view ->
            fragment.modifyGradeChooser(view, editorGrade) {
                listener.onClickEdit(editorGrade.id)
            }
        }

        val gradeColor = gradeNameToColor(editorGrade.name)

        holder.gradesListName.text = editorGrade.name
        holder.gradesListName.isSelected = true
        holder.gradesListName.setTextColor(if (ColorUtils.calculateLuminance(gradeColor) > 0.25) 0xaa000000.toInt() else 0xccffffff.toInt())
        holder.gradesListName.background.colorFilter = PorterDuffColorFilter(gradeColor, PorterDuff.Mode.MULTIPLY)
        holder.gradesListCategory.text = editorGrade.category
        if (editorGrade.weight < 0) {
            editorGrade.weight *= -1f
        }

        if (editorGrade.weight == 0f) {
            holder.gradesListWeight.text = app.getString(R.string.grades_weight_not_counted)
        } else {
            holder.gradesListWeight.text = app.getString(R.string.grades_weight_format, DecimalFormat("0.##").format(editorGrade.weight.toDouble()))
        }

        holder.gradesListValue.text = mContext.getString(R.string.grades_value_format, DecimalFormat("0.00").format(editorGrade.value.toDouble()))


        holder.buttonRemove.setOnClickListener { listener.onClickRemove(editorGrade.id) }

        holder.buttonEdit.setOnClickListener { v -> fragment.modifyGradeChooser(v, editorGrade) { listener.onClickEdit(editorGrade.id) } }
    }

    override fun getItemCount(): Int {
        return gradeList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var gradesListRoot: ConstraintLayout = itemView.findViewById(R.id.gradesListRoot)
        var gradesListName: TextView = itemView.findViewById(R.id.gradesListName)
        var gradesListWeight: TextView = itemView.findViewById(R.id.gradesListWeight)
        var gradesListValue: TextView = itemView.findViewById(R.id.gradesListValue)
        var gradesListCategory: TextView = itemView.findViewById(R.id.gradesListCategory)
        var buttonRemove: IconicsImageView = itemView.findViewById(R.id.buttonRemove)
        var buttonEdit: IconicsImageView = itemView.findViewById(R.id.buttonEdit)
    }
}

