package com.nux.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import com.nux.app.data.AppSpec
import com.nux.app.data.AppStore
import com.nux.app.ui.NuxApp
import com.nux.app.ui.NuxTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val spec = AppSpec.load(this)
        val needed = spec.runtimePermissions().filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
        setContent {
            val appSpec = remember { spec }
            val store = remember { AppStore(applicationContext) }
            NuxTheme(appSpec) {
                NuxApp(appSpec, store)
            }
        }
    }
}
