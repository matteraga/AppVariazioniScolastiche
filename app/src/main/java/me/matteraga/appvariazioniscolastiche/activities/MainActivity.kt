package me.matteraga.appvariazioniscolastiche.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.matteraga.appvariazioniscolastiche.R

class MainActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private lateinit var appBarConfiguration: AppBarConfiguration

    private var isLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            delay(100)
            isLoading = false
        }

        installSplashScreen().apply {
            setKeepOnScreenCondition {
                isLoading
            }
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val host: NavHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment? ?: return
        val navController = host.navController

        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        // Quando viene premuta la preference
        findNavController(R.id.nav_host_fragment).navigate(R.id.action_settingsFragment_to_checksFragment)
        return true
    }
}