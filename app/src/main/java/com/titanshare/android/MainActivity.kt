package com.titanshare.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.titanshare.android.ui.navigation.TitanNavGraph
import com.titanshare.android.ui.theme.NavyDeep
import com.titanshare.android.ui.theme.TitanShareTheme
import com.titanshare.android.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    // Activity-scoped reference so onResume can call into the ViewModel
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TitanShareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NavyDeep,
                ) {
                    val navController = rememberNavController()
                    TitanNavGraph(navController = navController, vm = vm)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Detect dropped connections after returning from another app
        vm.onAppResume()
    }
}
