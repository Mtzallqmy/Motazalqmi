package com.moataz.motazalqmi

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.moataz.motazalqmi.ui.BlogViewModel

/** Marks the process as offline before the app Application.onCreate runs.
 * This keeps bundled offline seed articles stable during device UI tests,
 * without disabling ordinary background sync in production or debug installs. */
class OfflineAndroidJUnitRunner : AndroidJUnitRunner() {
 override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
  System.setProperty("motazalqmi.test.offline", "true")
  BlogViewModel.autoSyncEnabled = false
  return super.newApplication(cl, className, context)
 }
}
