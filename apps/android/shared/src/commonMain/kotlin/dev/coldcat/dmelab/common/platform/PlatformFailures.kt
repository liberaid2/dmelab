package dev.coldcat.dmelab.common.platform

internal expect fun isPermissionFailure(error: Throwable): Boolean
internal expect fun isNativeLibraryFailure(error: Throwable): Boolean
