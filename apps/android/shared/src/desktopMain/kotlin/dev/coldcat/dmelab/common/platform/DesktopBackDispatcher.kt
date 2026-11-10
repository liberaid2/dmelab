package dev.coldcat.dmelab.common.platform

/** Window-level keyboard events enter the same handlers as the screen's Back button. */
object DesktopBackDispatcher {
    private val handlers = linkedMapOf<Any, () -> Boolean>()

    internal fun register(key: Any, handler: () -> Boolean) {
        handlers[key] = handler
    }

    internal fun unregister(key: Any) {
        handlers.remove(key)
    }

    fun onBack(): Boolean = handlers.values.toList().asReversed().any { it() }
}
