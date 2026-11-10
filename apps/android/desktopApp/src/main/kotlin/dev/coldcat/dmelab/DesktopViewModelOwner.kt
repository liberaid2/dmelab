package dev.coldcat.dmelab

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

internal class DesktopViewModelOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}
