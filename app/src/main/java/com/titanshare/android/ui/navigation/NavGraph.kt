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
import com.titanshare.android.ui.screens.SystemDetailsScreen
import com.titanshare.android.ui.screens.TrackpadScreen
import com.titanshare.android.ui.screens.WelcomeScreen
import com.titanshare.android.viewmodel.AppViewModel

@Composable
fun TitanNavGraph(navController: NavHostController, vm: AppViewModel) {
    val startDestination = if (vm.isWelcomeCompleted) Screen.Discovery.route else Screen.Welcome.route
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onGetStarted = {
                    vm.completeWelcome()
                    navController.navigate(Screen.Discovery.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onSkip = {
                    vm.completeWelcome()
                    navController.navigate(Screen.Discovery.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

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

        composable(Screen.SystemDetails.route) {
            SystemDetailsScreen(
                vm = vm,
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Trackpad.route) {
            TrackpadScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Screen.Keyboard.route) {
            KeyboardScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Screen.FileTransfer.route) {
            FileTransferScreen(
                vm = vm,
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LinuxFiles.route) {
            LinuxFilesScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Screen.Mirror.route) {
            MirrorScreen(vm = vm, onBack = { navController.popBackStack() })
        }
    }
}
