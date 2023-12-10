package me.matteraga.appvariazioniscolastiche.fragments

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import me.matteraga.appvariazioniscolastiche.ChangesToCheck
import me.matteraga.appvariazioniscolastiche.R
import me.matteraga.appvariazioniscolastiche.alarmmanager.AlarmScheduler
import me.matteraga.appvariazioniscolastiche.preferences.CheckPreference


class ChecksFragment : PreferenceFragmentCompat() {

    private lateinit var context: Context
    private lateinit var alarmScheduler: AlarmScheduler

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.context = context
        alarmScheduler = AlarmScheduler(context)
    }

    // Toolbar menu
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.top_checks_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.add -> createSeekBarView(alarmScheduler.getNotSetAlarms()[0], ChangesToCheck.TODAY)
                    // Torna alle impostazioni
                    else -> findNavController().navigate(R.id.action_checksFragment_to_settingsFragment)
                }
                return true
            }
        }, viewLifecycleOwner)
    }

    // Crea la view
    private fun showSeekBarView(hour: Int, changes: Int) {
        val newPref = CheckPreference(context).apply {
            title = "Controlla alle:"
            summary = "Le variazioni di:"
            showSeekBarValue = true
            min = 0
            max = 23
            value = hour
            changesToCheck = changes
            layoutResource = R.layout.check_preference
        }
        newPref.setOnPreferenceChangeListener { _, newValue ->
            // Controlla che non si già presente un allarme per l'ora
            if (!alarmScheduler.getScheduledAlarms().contains(newValue)) {
                // Il valore non è ancora stato modificato
                val changesToCheck = alarmScheduler.getScheduledAlarmChangesToCheck(newPref.value)
                alarmScheduler.cancelAndRemove(newPref.value)
                alarmScheduler.scheduleAndSave(newValue.toString().toInt(), changesToCheck)
                return@setOnPreferenceChangeListener true
            } else {
                Toast.makeText(
                    context,
                    "Non puoi eseguire più controlli alla stessa ora",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnPreferenceChangeListener false
            }
        }
        newPref.setOnChipsChangeListener {
            alarmScheduler.cancelAndRemove(newPref.value)
            alarmScheduler.scheduleAndSave(newPref.value, it)
        }
        newPref.setOnRemoveClickListener { preference ->
            // Un allarme rimane sempre
            if (alarmScheduler.getScheduledAlarms().count() > 1) {
                alarmScheduler.cancelAndRemove(preference.value)
                preferenceScreen.removePreference(preference)
            } else {
                Toast.makeText(
                    context,
                    "Puoi disattivare i controlli dalla home",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        preferenceScreen.addPreference(newPref)
    }

    // Crea l'allarme e la view, se ci sono menu di dieci allarmi già presenti
    private fun createSeekBarView(hour: Int, changes: Int) {
        if (alarmScheduler.getScheduledAlarms().count() >= 10) {
            Toast.makeText(context, "Non puoi aggiungere altri controlli", Toast.LENGTH_SHORT)
                .show()
            return
        }
        alarmScheduler.scheduleAndSave(hour, changes)
        showSeekBarView(hour, changes)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.checks_preferences, rootKey)

        // Carica la view di tutte le allarmi salvate
        alarmScheduler.getScheduledAlarms().forEach {
            showSeekBarView(it, alarmScheduler.getScheduledAlarmChangesToCheck(it))
        }
    }
}