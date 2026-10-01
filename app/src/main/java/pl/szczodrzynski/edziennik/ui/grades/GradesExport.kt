package pl.szczodrzynski.edziennik.ui.grades

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import pl.szczodrzynski.edziennik.ui.grades.compose.GradesUiState
import java.io.File
import java.util.Locale

object GradesExport {
    fun exportCsv(context: Context, state: GradesUiState) {
        val sb = StringBuilder()
        sb.append("Przedmiot;Ocena;Waga;Kategoria;Nauczyciel;Data;Komentarz\r\n")

        state.subjects.forEach { subject ->
            subject.grades.forEach { grade ->
                val row = listOf(
                    subject.name,
                    grade.value,
                    grade.weight.toString(),
                    grade.category ?: "",
                    grade.teacher ?: "",
                    grade.date ?: "",
                    grade.comment ?: ""
                ).joinToString(";") { "\"${it.replace("\"", "\"\"")}\"" }
                sb.append(row).append("\r\n")
            }
        }

        val file = File(context.cacheDir, "zestawienie-ocen.csv")
        file.writeText(sb.toString(), Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Zestawienie ocen Vision (.csv)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Eksportuj oceny (.csv)"))
    }
}
