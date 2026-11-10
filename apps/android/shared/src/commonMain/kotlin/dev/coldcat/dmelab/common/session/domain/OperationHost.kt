package dev.coldcat.dmelab.common.session.domain

/** Platform host starts foreground execution without owning admission or session state. */
fun interface OperationHost {
    fun start()
}
