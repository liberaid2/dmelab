package dev.coldcat.dmelab.common.platform

internal actual fun isPermissionFailure(error: Throwable): Boolean =
    error is SecurityException || error is java.nio.file.AccessDeniedException

internal actual fun isNativeLibraryFailure(error: Throwable): Boolean = error is LinkageError
