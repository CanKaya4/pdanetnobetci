package tr.nobetci.pdanet
import org.junit.Assert.*
import org.junit.Test
class PolicyTest {
    @Test fun knownNegativeButtonOnly() {
        assertTrue(Policy.dialogAllowed("com.pdanet", Policy.MESSAGE, Policy.MESSAGE, "android:id/button2", "Cancel", "Cancel"))
        assertFalse(Policy.dialogAllowed("com.pdanet", Policy.MESSAGE, Policy.MESSAGE, "android:id/button1", "Purchase Full Version", "Cancel"))
    }
    @Test fun foreignPackageNeverAllowed() { assertFalse(Policy.dialogAllowed("com.android.settings", Policy.MESSAGE, Policy.MESSAGE, "android:id/button2", "Cancel", "Cancel")) }
    @Test fun permissionDialogNeverMatches() { assertFalse(Policy.dialogAllowed("com.pdanet", "Grant location permission", Policy.MESSAGE, "android:id/button2", "Cancel", "Cancel")) }
    @Test fun partialMessageRejected() { assertFalse(Policy.dialogAllowed("com.pdanet", Policy.MESSAGE + " extra", Policy.MESSAGE, "android:id/button2", "Cancel", "Cancel")) }
    @Test fun backoffBoundedAndIncreasing() { assertEquals(1000L, Policy.backoff(0)); assertEquals(4000L, Policy.backoff(2)); assertEquals(60000L, Policy.backoff(100)) }
}
