package tr.nobetci.pdanet

enum class Phase { IDLE, CONNECTED, DISCONNECT_SUSPECTED, DISCONNECTED, DISMISS_DIALOG, RESTORE_TETHER, WAIT_FOR_PC, RECONNECTED, RECOVERY_FAILED, COOLDOWN }

object Policy {
    const val PACKAGE = "com.pdanet"
    const val MESSAGE = "Usage limit reached in free mode. Please unlock the full version or restart free mode."
    fun dialogAllowed(pkg: String, message: String, expected: String, buttonId: String, button: String, expectedButton: String): Boolean =
        pkg == PACKAGE && expected.length >= 20 && message == expected &&
            buttonId == "android:id/button2" && button == expectedButton &&
            expectedButton in setOf("Cancel", "İptal", "OK", "Tamam")
    fun backoff(attempt: Int): Long = minOf(60_000L, 1000L shl attempt.coerceIn(0, 6))
}
