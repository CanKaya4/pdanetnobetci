package tr.nobetci.pdanet

import android.content.Context
import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Store {
    private lateinit var app: Application
    private val changes = MutableStateFlow(0L)
    val updates = changes.asStateFlow()
    fun init(c: Context) { app = c.applicationContext as Application }
    val prefs get() = app.getSharedPreferences("guard", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(v) { prefs.edit().putBoolean("enabled", v).apply(); signal() }
    fun signal() { changes.value += 1 }
    fun value(key: String, fallback: String = "Bilinmiyor") = prefs.getString(key, fallback) ?: fallback
    fun put(key: String, value: String) { prefs.edit().putString(key, value).apply(); signal() }
    fun stamp(key: String) = put(key, SimpleDateFormat("dd.MM HH:mm:ss.SSS", Locale.ROOT).format(Date()))
    @Synchronized fun log(message: String) {
        val line = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT).format(Date()) + " " + message.take(1500)
        prefs.edit().putString("logs", (value("logs", "").lines().takeLast(199) + line).joinToString("\n")).apply()
        signal()
    }
    fun count(key: String) { prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply(); signal() }
    fun phase(p: Phase, reason: String) { put("phase", p.name); log("${p.name}: $reason") }
    fun installed(): String = try {
        val p = app.packageManager.getPackageInfo(Policy.PACKAGE, 0)
        "${p.packageName} / ${p.versionName} / ${p.longVersionCode}"
    } catch (_: android.content.pm.PackageManager.NameNotFoundException) { "Algılanmadı" }
    fun versionCode(): Long = try { app.packageManager.getPackageInfo(Policy.PACKAGE, 0).longVersionCode } catch (_: Exception) { -1L }
    fun verifiedVersion(): Boolean = versionCode() == 5232L
}
