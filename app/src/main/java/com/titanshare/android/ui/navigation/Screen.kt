package com.titanshare.android.ui.navigation

sealed class Screen(val route: String) {
    object Welcome      : Screen("welcome")
    object Discovery    : Screen("discovery")
    object Pairing      : Screen("pairing")
    object Dashboard    : Screen("dashboard")
    object Trackpad     : Screen("trackpad")
    object Keyboard     : Screen("keyboard")
    object FileTransfer : Screen("file_transfer")
    object LinuxFiles   : Screen("linux_files")
    object Mirror       : Screen("mirror")
}
