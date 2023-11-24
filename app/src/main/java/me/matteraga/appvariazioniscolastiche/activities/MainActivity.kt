package me.matteraga.appvariazioniscolastiche.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.judemanutd.autostarter.AutoStartPermissionHelper
import me.matteraga.appvariazioniscolastiche.R
import me.matteraga.appvariazioniscolastiche.fragments.SettingsFragment

class MainActivity : AppCompatActivity() {

    private fun showAutoStartDialog(context: Context) {
        // Controlla se il permesso di avvio automatico è disponibile
        val autoStartPermissionHelper = AutoStartPermissionHelper.getInstance()
        if (autoStartPermissionHelper.isAutoStartPermissionAvailable(
                context,
                true
            )
        ) {
            AlertDialog.Builder(context)
                .setTitle("Avvio automatico")
                .setMessage(
                    "Per funzionare correttamente è necessario che l'app abbia il permesso di eseguire in background."
                )
                // Apri la pagina di DontKillMyApp
                .setNeutralButton("Informazioni") { dialog, _ ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://dontkillmyapp.com")
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    }
                    startActivity(Intent.createChooser(intent, "Apri con"))
                    dialog.dismiss()
                }
                // Apri impostazioni esecuzione in background
                .setPositiveButton("Ok") { dialog, _ ->
                    autoStartPermissionHelper.getAutoStartPermission(
                        context,
                        true
                    )
                    dialog.dismiss()
                }
                .show()
        } else {
            Toast.makeText(context, "Non disponibile per questo dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.topAppBar))

        // Mostro il fragment delle impostazioni
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.frameLayout, SettingsFragment(), "settings")
            commit()
        }

        getPreferences(MODE_PRIVATE).apply {
            // Controllo se è la prima volta che l'app viene avviata
            if (!getBoolean("firstRun", false)) {
                // Richiede il permesso di avvio automatico
                showAutoStartDialog(this@MainActivity)
                // Imposto il flag firstRun a true
                with(edit()) {
                    putBoolean("firstRun", true)
                    apply()
                }
            }
        }
    }

    // Menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)
        return true
    }

    // Evento click sul menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.autoStart -> {
                showAutoStartDialog(this@MainActivity)
            }
        }
        return true
    }
}