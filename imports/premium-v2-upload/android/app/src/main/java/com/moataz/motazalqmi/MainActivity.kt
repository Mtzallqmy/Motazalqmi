package com.moataz.motazalqmi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.moataz.motazalqmi.ui.MainViewModel
import com.moataz.motazalqmi.ui.navigation.MotazNavGraph
import com.moataz.motazalqmi.ui.theme.MotazAlalqamiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = hiltViewModel()
            val settings by vm.readerSettings.collectAsState()
            MotazAlalqamiTheme(themeMode = settings.themeMode) {
                MotazNavGraph(vm = vm)
            }
        }
    }
}
