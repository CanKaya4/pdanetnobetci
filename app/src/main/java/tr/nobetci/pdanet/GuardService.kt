package tr.nobetci.pdanet

import android.app.*
import android.content.*
import android.net.*
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class GuardService : Service() {
    companion object {
        fun start(c: Context) {
            try { c.startForegroundService(Intent(c, GuardService::class.java)) }
            catch (e: Exception) { Store.log("Ön plan başlatma engeli: ${e.javaClass.simpleName}; uygulamayı açın") }
        }
    }
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { Store.log("Telefon ağı kullanılabilir; USB/PC başarısı anlamına gelmez") }
        override fun onLost(network: Network) { Store.log("Telefon varsayılan ağı kayboldu; USB durumuna kanıt değil") }
    }
    private var registered = false
    override fun onCreate() {
        super.onCreate(); Store.init(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("guard", "Bağlantı koruması", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, GuardService::class.java).setAction("stop"), PendingIntent.FLAG_IMMUTABLE)
        startForeground(1, Notification.Builder(this, "guard").setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("PdaNet Nöbetçi").setContentText("Koruma açık · USB olayları izleniyor")
            .setContentIntent(open).setOngoing(true).addAction(Notification.Action.Builder(null, "Durdur", stop).build()).build())
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(callback); registered = true
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "stop") Store.enabled = false
        if (!Store.enabled) { stopSelf(); return START_NOT_STICKY }
        return START_STICKY
    }
    override fun onDestroy() {
        if (registered) getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(callback)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}

class PdaNotifications : NotificationListenerService() {
    override fun onListenerConnected() { Store.init(this); Store.put("notificationListener", "Bağlı") }
    override fun onListenerDisconnected() { Store.put("notificationListener", "Bağlantı kesildi") }
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn?.packageName != Policy.PACKAGE) return
        Store.init(this)
        if (Store.enabled) Store.log("PdaNet bildirimi değişti; USB kopması olarak yorumlanmadı")
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Store.init(context)
        if (Store.enabled) { Store.phase(Phase.IDLE, "Yeniden başlatma; taze UI kanıtı bekleniyor"); GuardService.start(context) }
    }
}
