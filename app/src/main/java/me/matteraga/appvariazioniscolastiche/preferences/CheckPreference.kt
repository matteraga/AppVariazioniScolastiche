package me.matteraga.appvariazioniscolastiche.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import com.google.android.material.chip.ChipGroup
import me.matteraga.appvariazioniscolastiche.ChangesToCheck
import me.matteraga.appvariazioniscolastiche.R

class CheckPreference : SeekBarPreference {

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

    // Listener per rimuovere la preference
    private var removeClickListener: ((preference: CheckPreference) -> Unit)? = null

    // Listener per cambio selezione dei chips
    private var chipsChangeListener: ((changesToCheck: Int) -> Unit)? = null
    // Chips group
    private var chips: ChipGroup? = null
    // Chip selezionato
    var changesToCheck: Int = ChangesToCheck.TODAY
        set(value) {
            field = value
            // Aggiorna la ui, per ora non serve ma può tornare utile
            checkChips(value)
        }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val remove = holder.findViewById(R.id.remove)
        remove.setOnClickListener {
            removeClickListener?.invoke(this)
        }

        chips = holder.findViewById(R.id.chips) as ChipGroup
        checkChips(changesToCheck)
        chips?.setOnCheckedStateChangeListener { group, checkedIds ->
            // Aggiorna il chip selezionato, viene chiamata anche la funzione checkChips
            // ma essendo il chip già selezionato dall'utente non verrà chiamato nuovamente l'evento
            changesToCheck = chips?.indexOfChild(
                // Solo un chip può essere selezionato (SingleSelectionMode)
                group.findViewById(checkedIds.single())
            ) ?: ChangesToCheck.TODAY

            chipsChangeListener?.invoke(changesToCheck)
        }
    }

    // Seleziona il chip nella ui
    private fun checkChips(changesToCheck: Int) {
        when (changesToCheck) {
            ChangesToCheck.TODAY_AND_TOMORROW -> {
                chips?.check(R.id.both)
            }

            ChangesToCheck.TOMORROW -> {
                chips?.check(R.id.tomorrow)
            }

            else -> {
                chips?.check(R.id.today)
            }
        }
    }

    fun setOnRemoveClickListener(listener: (preference: CheckPreference) -> Unit) {
        removeClickListener = listener
    }

    fun setOnChipsChangeListener(listener: (changesToCheck: Int) -> Unit) {
        chipsChangeListener = listener
    }
}