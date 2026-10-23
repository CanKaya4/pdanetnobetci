package tr.nobetci.pdanet

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

class PdaAccessibility : AccessibilityService() {
    companion object { var instance: PdaAccessibility? = null; private set }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var recovery: Job? = null
    private var failures = 0
    private var nextAllowed = 0L
    private var lastClick = 0L
    private var started = 0L
    private var recovering = false
    private var lastSnapshot = ""
    private var lastClass = ""
    override fun onServiceConnected() {
        Store.init(this); instance = this
        Store.log("Erişilebilirlik bağlandı; eski kurtarma işi devam ettirilmedi")
        scope.launch {
            Store.updates.collect {
                if (!Store.enabled) { recovery?.cancel(); recovery = null; recovering = false }
            }
        }
        if (Store.enabled) GuardService.start(this)
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != Policy.PACKAGE) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) lastClass = event.className?.toString().orEmpty()
        inspect(event.eventTime)
    }
    private fun nodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val out = ArrayList<AccessibilityNodeInfo>()
        fun walk(n: AccessibilityNodeInfo, depth: Int) {
            if (depth > 25 || out.size >= 250) return
            out.add(n)
            for (i in 0 until n.childCount) n.getChild(i)?.let { walk(it, depth + 1) }
        }
        walk(root, 0); return out
    }
    private fun inspect(eventTime: Long = SystemClock.uptimeMillis()) {
        val root = rootInActiveWindow ?: run {
            Store.put("tether", "Okunamıyor: erişilebilir pencere yok"); return
        }
        if (root.packageName?.toString() != Policy.PACKAGE) return
        val all = nodes(root)
        val snapshot = "Pencere: $lastClass\n" + all.joinToString("\n") {
            "${it.viewIdResourceName} | ${it.className} | ${it.text ?: ""} | checked=${it.isChecked} checkable=${it.isCheckable}"
        }
        if (snapshot != lastSnapshot) { Store.put("diagnostics", snapshot.take(24000)); lastSnapshot = snapshot }
        if (!Store.enabled) return
        if (!Store.verifiedVersion() && (!Store.prefs.getBoolean("profileConfirmed", false) || Store.prefs.getLong("profileVersion", -2) != Store.versionCode())) {
            Store.put("tether", "Sürüm doğrulanmadı; tanılama profilini doğrulayın"); return
        }
        if (getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
            Store.put("tether", "Telefon kilitli; kullanıcı kilidi açmalı"); return
        }
        if (SystemClock.elapsedRealtime() < nextAllowed || SystemClock.elapsedRealtime() - lastClick < 750) return
        val expected = Store.value("message", Policy.MESSAGE)
        val message = all.firstOrNull { it.viewIdResourceName == "android:id/message" }?.text?.toString()
        val button = all.singleOrNull { it.viewIdResourceName == "android:id/button2" && it.isVisibleToUser && it.isEnabled }
        val title = all.firstOrNull { it.viewIdResourceName == "android:id/alertTitle" }?.text?.toString()
        if (message != null) {
            val trustedTitle = Store.value("title", "PdaNet+ has turned off")
            if (title == trustedTitle && button != null && Policy.dialogAllowed(Policy.PACKAGE, message, expected,
                    button.viewIdResourceName.orEmpty(), button.text?.toString().orEmpty(), Store.value("button", "Cancel"))) {
                begin("Doğrulanmış kesinti penceresi; olay gecikmesi=${SystemClock.uptimeMillis() - eventTime} ms")
                Store.phase(Phase.DISMISS_DIALOG, "Yalnızca olumsuz/onaylanmış düğme")
                lastClick = SystemClock.elapsedRealtime()
                if (!button.performAction(AccessibilityNodeInfo.ACTION_CLICK)) fail("Pencere düğmesi tıklanamadı")
            } else Store.put("tether", "Tanınmayan pencere: tanılama gerekli; işlem yapılmadı")
            return
        }
        // This preference has no stable app resource id. Require exact label, known activity,
        // an Android preference row, and exactly one explicit checkable control.
        if (lastClass != "com.foxfi.HotspotSettings") return
        val label = all.singleOrNull { it.text?.toString() == "USB Tether" && it.viewIdResourceName == "android:id/title" }
        val row = label?.parent?.parent ?: return
        val controls = nodes(row).filter { it.isCheckable && it.isVisibleToUser }
        if (controls.size != 1 || nodes(row).none { it.text?.toString() == "USB Tether" }) {
            Store.put("tether", "USB kontrolü belirsiz; tanılama gerekli"); return
        }
        val control = controls.single()
        Store.put("tether", if (control.isChecked) "Açık (UI); PC bağlantısı bilinmiyor" else "Kapalı (UI)")
        if (control.isChecked) {
            if (recovering) {
                Store.phase(Phase.WAIT_FOR_PC, "USB açık doğrulandı; Windows bağımsız doğrulamalı")
                // A checked box proves only phone readiness, not end-to-end reconnection.
                Store.stamp("phoneReady"); recovering = false; recovery?.cancel(); recovery = null
                Store.count("phoneSuccess"); Store.log("Telefon USB hazır doğrulandı; kurtarma=${SystemClock.elapsedRealtime() - started} ms; PC bilinmiyor")
                failures = 0; nextAllowed = SystemClock.elapsedRealtime() + 3000
            }
            return
        }
        begin("USB kapalı doğrulandı; olay gecikmesi=${SystemClock.uptimeMillis() - eventTime} ms")
        Store.phase(Phase.RESTORE_TETHER, "Kapalı USB Tether açılıyor")
        lastClick = SystemClock.elapsedRealtime()
        val clicked = if (control.isClickable) control.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            else if (row.isClickable) row.performAction(AccessibilityNodeInfo.ACTION_CLICK) else false
        if (!clicked) fail("USB satırı tıklanabilir değil")
    }
    private fun begin(reason: String) {
        if (recovering) return
        recovering = true; started = SystemClock.elapsedRealtime()
        Store.stamp("lastDisconnect"); Store.stamp("lastAttempt")
        Store.phase(Phase.DISCONNECTED, reason)
        recovery = scope.launch {
            delay(900); inspect()
            delay(2100); inspect()
            delay(7000)
            if (recovering && Store.enabled) fail("10 sn içinde USB açık doğrulanamadı; kilit/arka plan/erişim engeli olabilir")
        }
    }
    private fun fail(reason: String) {
        recovering = false; recovery?.cancel(); recovery = null
        failures++; nextAllowed = SystemClock.elapsedRealtime() + Policy.backoff(failures)
        Store.count("failed"); Store.phase(Phase.RECOVERY_FAILED, "$reason; geçen=${SystemClock.elapsedRealtime() - started} ms")
        Store.phase(Phase.COOLDOWN, "${Policy.backoff(failures)} ms bekleme")
        scope.launch { delay(Policy.backoff(failures)); if (Store.enabled) inspect() }
    }
    fun manual() {
        if (!Store.enabled) { Store.log("Önce korumayı açın"); return }
        nextAllowed = 0
        try {
            val launch = packageManager.getLaunchIntentForPackage(Policy.PACKAGE)
            if (launch == null) Store.log("PdaNet bulunamadı")
            else { startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); scope.launch { delay(800); inspect() } }
        } catch (e: Exception) { Store.log("PdaNet açılamadı: ${e.javaClass.simpleName}") }
    }
    override fun onInterrupt() { stopSafely("Erişilebilirlik kesildi") }
    private fun stopSafely(reason: String) {
        recovering = false; recovery?.cancel(); Store.enabled = false
        Store.phase(Phase.IDLE, reason); stopService(Intent(this, GuardService::class.java))
    }
    override fun onUnbind(intent: Intent?): Boolean { stopSafely("Erişilebilirlik izni kaldırıldı"); return super.onUnbind(intent) }
    override fun onDestroy() { instance = null; scope.cancel(); super.onDestroy() }
}
