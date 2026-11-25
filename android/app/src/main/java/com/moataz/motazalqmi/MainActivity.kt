package com.moataz.motazalqmi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moataz.motazalqmi.ui.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState); enableEdgeToEdge()
  val uri = intent?.data
  val slug = if (uri?.scheme == "https" && uri.host == "motazalqmi.vercel.app" && uri.pathSegments.size == 2) when (uri.pathSegments[0]) {
   "articles" -> uri.pathSegments[1]
   "r" -> "r/" + uri.pathSegments[1]
   else -> null
  } else null
  setContent {
   val vm: BlogViewModel = hiltViewModel()
   val settings by vm.settings.collectAsStateWithLifecycle()
   BlogTheme(settings.theme) { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { BlogApp(vm, slug) } }
  }
 }
}
