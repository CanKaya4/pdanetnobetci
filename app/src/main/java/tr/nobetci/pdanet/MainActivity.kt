package tr.nobetci.pdanet

import android.Manifest
import android.app.*
import android.content.*
import android.os.*
import android.provider.Settings
import android.view.View
import android.widget.*
import kotlinx.coroutines.*

class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var status: TextView
    private lateinit var history: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); Store.init(this)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 40, 32, 32) }
        setContentView(ScrollView(this).apply { addView(column) })
        fun text(s: String, size: Float = 16f): TextView = TextView(this).apply { text = s; textSize = size; setPadding(0, 12, 0, 12); column.addView(this) }
        fun button(s: String, action: () -> Unit) { column.addView(Button(this).apply { text = s; setOnClickListener { action() } }) }
        text("PdaNet Nöbetçi", 28f)
        text("USB bağlantı koruması · v0.1.0\nTelefon ve Windows ayrı izlenir. Kesintisiz oyun bağlantısı garanti edilemez.")
        status = text("")
        button("Korumayı aç / kapat") {
            if (!Store.enabled && PdaAccessibility.instance == null) {
                AlertDialog.Builder(this).setMessage("Önce erişilebilirlik izni verin. Yalnızca PdaNet ekranı okunur ve doğrulanmış USB/kesinti kontrolleri kullanılır.").setPositiveButton("Ayarlar") { _, _ -> open(Settings.ACTION_ACCESSIBILITY_SETTINGS) }.setNegativeButton("İptal", null).show()
            } else {
                Store.enabled = !Store.enabled
                if (Store.enabled) { GuardService.start(this); Store.phase(Phase.IDLE, "Taze PdaNet olayı bekleniyor") }
                else { stopService(Intent(this, GuardService::class.java)); Store.phase(Phase.IDLE, "Kullanıcı durdurdu") }
            }
        }
        button("Manuel yeniden bağlanmayı dene") { PdaAccessibility.instance?.manual() ?: Store.log("Erişilebilirlik bağlı değil") }
        button("İlk kurulum rehberi / izinler") { guide() }
        button("Tanılama ve mesaj deseni") { diagnostics() }
        text("Olay geçmişi (en fazla 200 kayıt)", 20f)
        history = text("").apply { setTextIsSelectable(true) }
        scope.launch { Store.updates.collect { refresh() } }
    }
    override fun onResume() { super.onResume(); refresh() }
    private fun refresh() {
        status.text = "Koruma: ${if (Store.enabled) "Açık" else "Kapalı"}\n" +
            "PdaNet: ${Store.installed()}\nUSB: ${Store.value("tether")}\nDurum: ${Store.value("phase", "IDLE")}\n" +
            "Son kopma: ${Store.value("lastDisconnect")}\nSon deneme: ${Store.value("lastAttempt")}\n" +
            "Telefon USB hazır: ${Store.value("phoneReady")}\nSon başarılı PC bağlantısı: Android'den doğrulanamaz\n" +
            "Başarılı telefon kurtarması: ${Store.prefs.getInt("phoneSuccess", 0)} · Başarısız telefon denemesi: ${Store.prefs.getInt("failed", 0)}\nBaşarılı uçtan uca: bilinmiyor\n" +
            "Erişilebilirlik: ${if (PdaAccessibility.instance != null) "Bağlı" else "Kapalı"}\n" +
            "Bildirim gösterme: ${getSystemService(NotificationManager::class.java).areNotificationsEnabled()}\n" +
            "Bildirim erişimi: ${Store.value("notificationListener")}\n" +
            "Pil istisnası: ${getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)}"
        history.text = Store.value("logs", "Henüz olay yok")
    }
    private fun open(action: String) { try { startActivity(Intent(action)) } catch (_: ActivityNotFoundException) { Store.log("Bu ayar cihazda bulunamadı: $action") } }
    private fun guide() {
        val items = arrayOf("1 · Erişilebilirlik izni", "2 · Bildirim erişimi (isteğe bağlı)", "3 · Bildirim gösterme izni", "4 · Pil optimizasyonu ayarları", "Galaxy A56 kurulum açıklaması")
        AlertDialog.Builder(this).setTitle("İlk kurulum").setItems(items) { _, which -> when (which) {
            0 -> open(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            1 -> open(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            2 -> if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            3 -> open(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            else -> AlertDialog.Builder(this).setMessage("Ayarlar → Erişilebilirlik → Yüklü uygulamalar → PdaNet Nöbetçi. APK kaynağı nedeniyle kısıtlı ayar uyarısı varsa Uygulama bilgisi menüsünden kısıtlı ayarlara izin verin.\n\nBildirim erişimi yalnızca PdaNet bildirim olaylarını kaydeder; bildirim içeriğini saklamaz ve zorunlu değildir.\n\nPil → Arka plan kullanım sınırları → Asla uyumayan uygulamalar listesine ekleyin (menü adı One UI sürümüne bağlıdır). Özel otomatik başlatma izni yoktur; açılış alıcısı ve erişilebilirlik yeniden bağlanması kullanılır. Zorla durdurmadan sonra uygulamayı yeniden açın.\n\nPdaNet USB Tether ve PC bağlantısını ilk kez elle kurun. Ardından korumayı açın. Telefon kilidi otomatik açılmaz; uygulamalar arası başlatma Android tarafından engellenebilir. Tanılama okunamıyorsa elle müdahale gerekir.").setPositiveButton("Tamam", null).show()
        } }.show()
    }
    private fun diagnostics() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 16, 24, 16) }
        val info = TextView(this).apply { text = "PdaNet: ${Store.installed()}\nYalnızca doğrulanmış, zararsız kesinti metnini kaydedin. Satın alma veya izin ekranını kaydetmeyin. Kullanılan düğme yalnızca android:id/button2 olabilir.\n\n" + Store.value("diagnostics", "PdaNet'i açıp mesajı görünür yapın; sonra buraya dönün."); setTextIsSelectable(true) }
        layout.addView(info)
        fun field(hint: String, key: String, fallback: String): EditText = EditText(this).apply { this.hint = hint; setText(Store.value(key, fallback)); layout.addView(this) }
        val message = field("Tam mesaj", "message", Policy.MESSAGE)
        val title = field("Tam pencere başlığı", "title", "PdaNet+ has turned off")
        val button = field("Güvenli düğme metni", "button", "Cancel")
        val confirm = CheckBox(this).apply { text = "Bu sürümde mesajın yalnızca kesinti bildirimi olduğunu ve button2'nin zararsız olduğunu doğruladım"; layout.addView(this) }
        AlertDialog.Builder(this).setTitle("Yerel tanılama / tam eşleşme").setView(ScrollView(this).apply { addView(layout) }).setNegativeButton("Kapat", null).setPositiveButton("Profili kaydet") { _, _ ->
            if (confirm.isChecked && message.text.length >= 20 && button.text.toString() in setOf("Cancel", "İptal", "OK", "Tamam")) {
                Store.put("message", message.text.toString()); Store.put("title", title.text.toString()); Store.put("button", button.text.toString())
                Store.prefs.edit().putBoolean("profileConfirmed", true).putLong("profileVersion", Store.versionCode()).apply(); Store.log("Kullanıcı bu sürümün mesaj profilini doğruladı")
            } else Store.log("Profil kaydedilmedi: doğrulama ve geçerli alanlar gerekli")
        }.show()
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
