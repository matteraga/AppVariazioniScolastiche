package me.matteraga.appvariazioniscolastiche.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
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

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val remove = holder.findViewById(R.id.remove)
        remove.setOnClickListener {
            removeClickListener?.invoke(this)
        }
    }

    fun setOnRemoveClickListener(listener: (preference: CheckPreference) -> Unit) {
        removeClickListener = listener
    }
}