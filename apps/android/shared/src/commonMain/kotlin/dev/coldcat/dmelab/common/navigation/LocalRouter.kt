package dev.coldcat.dmelab.common.navigation

import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalRouter = staticCompositionLocalOf<Router> { error("Router is not provided") }
