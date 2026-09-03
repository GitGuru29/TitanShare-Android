package com.titanshare.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.titanshare.android.ui.screens.DashboardScreen
import com.titanshare.android.ui.screens.DiscoveryScreen
import com.titanshare.android.ui.screens.FileTransferScreen
import com.titanshare.android.ui.screens.KeyboardScreen
import com.titanshare.android.ui.screens.LinuxFilesScreen
import com.titanshare.android.ui.screens.MirrorScreen
import com.titanshare.android.ui.screens.PairingScreen
import com.titanshare.android.ui.screens.TrackpadScreen
import com.titanshare.android.viewmodel.AppViewModel

@Composable
fun TitanNavGraph(navController: NavHostController, vm: AppViewModel) {
    NavHost(navController = navController, startDestination = Screen.Discovery.route) {

        composable(Screen.Discovery.route) {
            DiscoveryScreen(vm = vm, onDeviceSelected = {
                navController.navigate(Screen.Pairing.route)
            })
        }

        composable(Screen.Pairing.route) {
            PairingScreen(
                vm = vm,
                onConnected = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Discovery.route) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                vm = vm,
                onNavigate = { route -> navController.navigate(route) },
                onDisconnect = {
                    navController.navigate(Screen.Discovery.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Trackpad.route) {
            TrackpadScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Screen.Keyboard.route) {
            KeyboardScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Screen.FileTransfer.route) {
            FileTransferScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Screen.LinuxFiles.route) {
            LinuxFilesScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Screen.Mirror.route) {
            MirrorScreen(vm = vm, onBack = { navController.popBackStack() })
        }
    }
}
