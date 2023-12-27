package me.matteraga.appvariazioniscolastiche.fragments

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.judemanutd.autostarter.AutoStartPermissionHelper
import me.matteraga.appvariazioniscolastiche.ChangesToCheck
import me.matteraga.appvariazioniscolastiche.R
import me.matteraga.appvariazioniscolastiche.alarmmanager.AlarmScheduler
import me.matteraga.appvariazioniscolastiche.preferences.DaysListPreference
import me.matteraga.appvariazioniscolastiche.utilities.StorageUtils
import me.matteraga.appvariazioniscolastiche.workers.CheckChangesWorker
import me.matteraga.appvariazioniscolastiche.workers.DeletePdfsWorker

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var context: Context

    private lateinit var sharedPrefFlags: SharedPreferences
    private lateinit var sharedPrefFiles: SharedPreferences

    private lateinit var workManager: WorkManager
    private lateinit var storageUtils: StorageUtils
    private lateinit var alarmScheduler: AlarmScheduler

    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var filesPermissionsLauncher: ActivityResultLauncher<Array<String>>

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.context = context

        sharedPrefFlags = context.getSharedPreferences("flags", Context.MODE_PRIVATE)
        sharedPrefFiles = context.getSharedPreferences("files", Context.MODE_PRIVATE)

        workManager = WorkManager.getInstance(context)
        storageUtils = StorageUtils(context)
        alarmScheduler = AlarmScheduler(context)

        // Richiede il permesso per le notifiche
        notificationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (!isGranted) {
                    AlertDialog.Builder(context).apply {
                        setTitle("Permesso non concesso")
                        setMessage("Non potrai ricevere notifiche quando ci sono variazioni.")
                        setNeutralButton("Ok") { dialog, _ ->
                            dialog.dismiss()
                        }
                    }.create().show()
                }
            }

        // Richiede il permesso per i file
        filesPermissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == false ||
                    permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == false
                ) {
                    AlertDialog.Builder(context).apply {
                        setTitle("Permesso non concesso")
                        setMessage("Non potrai salvare i PDF delle variazioni.")
                        setNeutralButton("Ok") { dialog, _ ->
                            dialog.dismiss()
                        }
                    }.create().show()
                }
            }
    }

    private fun showAutoStartDialog(context: Context) {
        if (AutoStartPermissionHelper.getInstance()
                .isAutoStartPermissionAvailable(context, false)
        ) {
            AlertDialog.Builder(context).apply {
                setTitle("Don't kill my app!")
                setMessage("Per funzionare correttamente è necessario che l'app possa eseguire in background.")
                setNeutralButton("Scopri come fare") { dialog, _ ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://dontkillmyapp.com")
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    }
                    startActivity(Intent.createChooser(intent, "Apri con"))
                    dialog.dismiss()
                }
            }.create().show()
            return
        }
        Toast.makeText(context, "Non necessario per questo dispositivo", Toast.LENGTH_SHORT)
            .show()
    }

    // Toolbar menu
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.top_settings_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.autoStart -> showAutoStartDialog(context)
                }
                return true
            }
        }, viewLifecycleOwner)
    }

    private fun enqueueDeletePdfsWork() {
        workManager.enqueueUniqueWork(
            "delete",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DeletePdfsWorker>()
                .build()
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)

        // Autostart dialog e richiesta permesso mostrati solo la prima volta
        if (sharedPrefFlags.getBoolean("firstRun", true)) {
            showAutoStartDialog(context)
            // Se android >= 13 richiedi permesso per le notifiche
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            // Se android < 29 richiedi storage
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                filesPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
            with(sharedPrefFlags.edit()) {
                putBoolean("firstRun", false)
                apply()
            }
        }

        // Apre il PDF delle variazioni del giorno selezionato
        val openPreference = findPreference<DaysListPreference>("open")
        openPreference?.setOnPreferenceChangeListener { _, newValue ->
            // new value è la data selezionata
            val pref = sharedPrefFiles.getString("${newValue}-uri", "") ?: ""
            val uri = Uri.parse(pref)
            if (pref.isNotBlank() && storageUtils.check(uri)) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                startActivity(Intent.createChooser(intent, "Apri con"))
            } else {
                Toast.makeText(context, "File non trovato", Toast.LENGTH_SHORT).show()
            }
            true
        }

        // Controlla se ci sono variazioni per il giorno selezionato
        val manualCheckPreference = findPreference<DaysListPreference>("manualCheck")
        manualCheckPreference?.setOnPreferenceChangeListener { _, newValue ->
            val data = Data.Builder().apply {
                putString("date", newValue.toString())
                putBoolean("openPDF", true)
            }.build()
            // Il worker si aspetta la data per cui controllare le variazioni
            workManager.enqueueUniqueWork(
                "manual-check-${newValue}",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<CheckChangesWorker>().apply {
                    setInputData(data)
                }.build()
            )
            Toast.makeText(context, "Controllo le variazioni", Toast.LENGTH_SHORT).show()
            true
        }

        // Attiva/disattiva le varie allarmi, non vengono rimosse dalle preference solo disattivate
        val checkChangesPreference = findPreference<SwitchPreferenceCompat>("check")
        checkChangesPreference?.setOnPreferenceChangeListener { _, newValue ->
            val newValueBool = newValue.toString().toBoolean()
            if (newValueBool) {
                // Un allarme è sempre presente
                if (alarmScheduler.getScheduledAlarms().isEmpty()) {
                    alarmScheduler.scheduleAndSave(19, ChangesToCheck.TODAY)
                }
                alarmScheduler.scheduleSaved()
            } else {
                alarmScheduler.cancelSaved()
            }
            true
        }

        // Imposta la classe, il maiuscolo è solo estetico il controllo è case insensitive
        val schoolClassPreference = findPreference<EditTextPreference>("schoolClass")
        val regex = Regex("""[1-5][A-Za-z]{1,3}""")
        schoolClassPreference?.setOnBindEditTextListener { editText ->
            editText.filters = arrayOf(InputFilter.AllCaps(), InputFilter.LengthFilter(4))
        }
        schoolClassPreference?.setOnPreferenceChangeListener { _, newValue ->
            if (regex.matches(newValue.toString())) {
                // Se cambia la classe elimina i pdf salvati perché evidenziati
                enqueueDeletePdfsWork()
                return@setOnPreferenceChangeListener true
            } else {
                Toast.makeText(context, "Classe non valida", Toast.LENGTH_SHORT).show()
                return@setOnPreferenceChangeListener false
            }
        }

        // Gestisce il permesso per le notifiche
        val notifyPreference = findPreference<Preference>("notify")
        notifyPreference?.setOnPreferenceClickListener {
            if ((!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_DENIED) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                // Se puoi chiedere il permesso e non è concesso e hai Android >= 12
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Altrimenti apri le impostazioni dell'app nelle notifiche
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().packageName)
                }
                startActivity(intent)
            }
            true
        }

        // Gestisce il permesso per i file, solo per Android < 10
        val filesPreference = findPreference<Preference>("files")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            filesPreference?.setOnPreferenceClickListener {
                val write = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED

                val read = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED

                if ((!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) && write) ||
                    (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) && read)
                ) {
                    // Se puoi chiedere il permesso e non è concesso
                    val permissionsToRequest = mutableListOf<String>()
                    if (write) {
                        permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    if (read) {
                        permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    if (permissionsToRequest.isNotEmpty()) {
                        filesPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                } else {
                    // Altrimenti apri le impostazioni dell'app
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + requireActivity().packageName)
                    )
                    startActivity(intent)
                }
                true
            }
        } else {
            filesPreference?.isVisible = false
        }

        // Gestisce il permesso per le allarmi precise, solo per Android >= 12
        val exactAlarmsPreference = findPreference<Preference>("exactAlarms")
        // Gestisce l'attivazione delle allarmi precise, solo per Android >= 12
        val useExactAlarmsPreference = findPreference<SwitchPreferenceCompat>("useExactAlarms")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            exactAlarmsPreference?.setOnPreferenceClickListener {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                true
            }
            useExactAlarmsPreference?.setOnPreferenceChangeListener { preference, newValue ->
                if (newValue.toString().toBoolean()) {
                    AlertDialog.Builder(context).apply {
                        setTitle("Avviso")
                        setMessage("Le allarmi precise consumano più batteria rispetto alle allarmi normali. Attivale solo se vuoi che il controllo delle variazioni sia preciso.")
                        setPositiveButton("Prosegui") { dialog, _ ->
                            (preference as SwitchPreferenceCompat).isChecked = true
                            AlarmScheduler(context).scheduleSaved()
                            dialog.dismiss()
                        }
                        setNegativeButton("Annulla") { dialog, _ ->
                            (preference as SwitchPreferenceCompat).isChecked = false
                            dialog.dismiss()
                        }
                    }.create().show()
                } else {
                    (preference as SwitchPreferenceCompat).isChecked = false
                    AlarmScheduler(context).scheduleSaved()
                }
                false
            }
        } else {
            exactAlarmsPreference?.isVisible = false
            useExactAlarmsPreference?.isVisible = false
        }

        // Elimina i PDF delle variazioni e le shared preferences (uri dei pdf)
        val deletePdfsPreference = findPreference<Preference>("delete")
        deletePdfsPreference?.setOnPreferenceClickListener {
            enqueueDeletePdfsWork()
            Toast.makeText(context, "Elimino i file", Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun onResume() {
        super.onResume()

        // Aggiorna se il permesso per le notifiche è concesso o meno
        val notifyPreference = findPreference<Preference>("notify")
        notifyPreference?.summary = if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            "Concesso"
        } else {
            "Negato"
        }

        // Aggiorna se il permesso per i file è concesso o meno
        val filesPreference = findPreference<Preference>("files")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            filesPreference?.summary = if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                "Concesso"
            } else {
                "Negato"
            }
        }

        // Aggiorna se il permesso per le allarmi precise è concesso o meno, attiva o disattiva lo switch, solo per Android >= 12
        val exactAlarmsPreference = findPreference<Preference>("exactAlarms")
        val useExactAlarmsPreference = findPreference<SwitchPreferenceCompat>("useExactAlarms")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            exactAlarmsPreference?.summary =
                if (context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
                    useExactAlarmsPreference?.isEnabled = true
                    "Concesso"
                } else {
                    useExactAlarmsPreference?.isEnabled = false
                    useExactAlarmsPreference?.isChecked = false
                    "Negato"
                }
        }
    }
}