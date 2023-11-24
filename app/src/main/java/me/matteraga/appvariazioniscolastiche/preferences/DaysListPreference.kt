package me.matteraga.appvariazioniscolastiche.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import me.matteraga.appvariazioniscolastiche.R
import java.time.LocalDate

class DaysListPreference : ListPreference {

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context)

    // La ListPreference di base mostra solo una lista di radio button,
    // quindi creo un dialog con una lista items senza radio button
    override fun onClick() {
        // Assegna i valori ai giorni (Oggi, domani) della lista
        entryValues = arrayOf(LocalDate.now().toString(), LocalDate.now().plusDays(1L).toString())

        // Mostra la lista dei giorni senza radio button
        AlertDialog.Builder(context).apply {
            setTitle(title)
            setItems(R.array.days) { dialog, index ->
                if (callChangeListener(entryValues[index].toString())) {
                    setValueIndex(index)
                }
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        }.show()
    }
}