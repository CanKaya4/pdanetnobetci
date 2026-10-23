using System;
using System.IO;
using System.Linq;
using System.Diagnostics;
using System.Net.NetworkInformation;
using System.Threading;
using System.Windows.Forms;
using System.Windows.Automation;

public sealed class Nobetci : ApplicationContext {
    [STAThread]
    public static int Main(string[] args) {
        if (args.Contains("--self-test")) {
            return Delay(0) == 1000 && Delay(100) == 60000 && IsConnected(OperationalStatus.Up) && !IsConnected(OperationalStatus.Down) ? 0 : 1;
        }
        if (args.Contains("--install-startup") || args.Contains("--remove-startup")) {
            SetStartup(args.Contains("--install-startup")); return 0;
        }
        Run(); return 0;
    }
    static void SetStartup(bool enabled) {
        using (var key = Microsoft.Win32.Registry.CurrentUser.CreateSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run")) {
            if (enabled) key.SetValue("PdaNetNobetci", "\"" + Application.ExecutablePath + "\"");
            else key.DeleteValue("PdaNetNobetci", false);
        }
    }
    readonly NotifyIcon icon = new NotifyIcon();
    readonly System.Windows.Forms.Timer timer = new System.Windows.Forms.Timer();
    readonly string logPath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "PdaNetNobetci", "windows.log");
    volatile bool networkChanged = true;
    DateTime next = DateTime.MinValue, suspect = DateTime.MinValue, outage = DateTime.MinValue;
    int attempts = 0, successes = 0;
    bool recovering = false;
    bool enabled = true;
    const string AdapterName = "PdaNet Broadband Connection";
    public static int Delay(int n) { return (int)Math.Min(60000, 1000 * Math.Pow(2, Math.Min(n,6))); }
    public static bool IsConnected(OperationalStatus status) { return status == OperationalStatus.Up; }
    public Nobetci() {
        icon.Icon = System.Drawing.SystemIcons.Application;
        icon.Text = "PdaNet Nobetci"; icon.Visible = true;
        var menu = new ContextMenuStrip();
        menu.Items.Add("Korumayi ac / kapat", null, delegate { enabled = !enabled; networkChanged = true; Log("Koruma: " + enabled); });
        menu.Items.Add("Olay kaydini ac", null, delegate { if (File.Exists(logPath)) Process.Start("notepad.exe", logPath); });
        menu.Items.Add("Oturum acilisinda baslat", null, delegate { SetStartup(true); });
        menu.Items.Add("Otomatik baslatmayi kaldir", null, delegate { SetStartup(false); });
        menu.Items.Add("Cikis", null, delegate { ExitThread(); }); icon.ContextMenuStrip = menu;
        NetworkChange.NetworkAddressChanged += Changed;
        NetworkChange.NetworkAvailabilityChanged += Availability;
        timer.Interval = 250; timer.Tick += Tick; timer.Start();
        Log("Basladi. Adaptore reset uygulanmaz; yalnizca dogrulanmis UIA Connect menusu kullanilir.");
    }
    void Changed(object sender, EventArgs args) { networkChanged = true; }
    void Availability(object sender, NetworkAvailabilityEventArgs args) { networkChanged = true; }
    void Log(string s) {
        Directory.CreateDirectory(Path.GetDirectoryName(logPath));
        if (File.Exists(logPath) && new FileInfo(logPath).Length > 262144) File.WriteAllText(logPath, "");
        File.AppendAllText(logPath, DateTime.Now.ToString("O") + " " + s + Environment.NewLine);
    }
    void Tick(object sender, EventArgs args) {
        if (!enabled) return;
        if (!networkChanged && !recovering && suspect == DateTime.MinValue) return;
        if (!networkChanged && DateTime.UtcNow < next) return;
        networkChanged = false;
        try {
            var adapters = NetworkInterface.GetAllNetworkInterfaces().Where(n => n.Name == AdapterName && n.Description.IndexOf("PdaNet", StringComparison.OrdinalIgnoreCase) >= 0).ToArray();
            if (adapters.Length != 1) {
                if (!recovering) Log("Adapter yok/belirsiz; tahmin edilmedi. PdaNet surucusunu ve adapter adini denetleyin.");
                if (!recovering) outage = DateTime.UtcNow;
                recovering = true; next = DateTime.UtcNow.AddSeconds(30); return;
            }
            if (IsConnected(adapters[0].OperationalStatus)) {
                if (recovering) { successes++; Log("RECONNECTED: Adapter Up. Internet/oyun sunucusu dogrulanmadi. Gecen ms=" + (DateTime.UtcNow-outage).TotalMilliseconds + "; basarili=" + successes); }
                recovering = false; attempts = 0; suspect = DateTime.MinValue; return;
            }
            if (suspect == DateTime.MinValue) { suspect = DateTime.UtcNow; next = suspect.AddMilliseconds(750); Log("DISCONNECT_SUSPECTED"); return; }
            if (!recovering) { recovering = true; outage = DateTime.UtcNow; Log("DISCONNECTED: 750 ms onaylandi"); }
            if (DateTime.UtcNow < next) return;
            // Recheck immediately before interacting. Never toggle an Up adapter.
            if (NetworkInterface.GetAllNetworkInterfaces().Any(n => n.Id == adapters[0].Id && IsConnected(n.OperationalStatus))) { networkChanged = true; return; }
            var result = TryConnect(); attempts++;
            next = DateTime.UtcNow.AddMilliseconds(Delay(attempts));
            Log("Deneme=" + attempts + "; " + result + "; sonraki ms=" + Delay(attempts));
        } catch (Exception e) { Log("Hata: " + e.GetType().Name + " " + e.Message); recovering = true; next = DateTime.UtcNow.AddSeconds(30); }
    }
    static string TryConnect() {
        var processes = Process.GetProcessesByName("PdaNetPC");
        if (processes.Length != 1) return "PdaNetPC tekil olarak bulunamadi; uygulamayi acin";
        string path;
        try { path = processes[0].MainModule.FileName; } catch { return "Islem yolu okunamadi"; }
        if (!string.Equals(Path.GetFileName(path), "PdaNetPC.exe", StringComparison.OrdinalIgnoreCase)) return "Islem kimligi uyusmuyor";
        // First use a visible menu owned by the verified process.
        if (InvokeConnect(processes[0].Id)) return "Connect USB UIA Invoke gonderildi; sonuc bekleniyor";
        // Reveal the tray context menu through the accessible icon, never screen coordinates.
        var icons = AutomationElement.RootElement.FindAll(TreeScope.Descendants,
            new PropertyCondition(AutomationElement.ControlTypeProperty, ControlType.Button));
        var candidates = icons.Cast<AutomationElement>().Where(e => {
            try { return e.Current.Name.StartsWith("PdaNet", StringComparison.OrdinalIgnoreCase) &&
                !e.Current.IsOffscreen && Process.GetProcessById(e.Current.ProcessId).ProcessName == "explorer"; }
            catch { return false; }
        }).ToArray();
        if (candidates.Length != 1) return "Tekil gorunur PdaNet tepsi simgesi yok; simgeyi her zaman gorunur yapin";
        try { candidates[0].SetFocus(); SendKeys.SendWait("+{F10}"); }
        catch (Exception e) { return "Tepsi menusu acilamadi: " + e.GetType().Name; }
        // UIA menu creation can be asynchronous. A short bounded wait happens only during outage.
        for (int i=0;i<5;i++) { Thread.Sleep(100); if (InvokeConnect(processes[0].Id)) return "Connect USB UIA Invoke gonderildi"; }
        return "PdaNet menusunde desteklenen InvokePattern bulunamadi; tiklama yapilmadi";
    }
    static bool InvokeConnect(int pid) {
        var condition = new AndCondition(new PropertyCondition(AutomationElement.ProcessIdProperty,pid),
            new PropertyCondition(AutomationElement.ControlTypeProperty,ControlType.MenuItem));
        var items = AutomationElement.RootElement.FindAll(TreeScope.Descendants,condition).Cast<AutomationElement>()
            .Where(e => !e.Current.IsOffscreen && e.Current.IsEnabled &&
                new [] {"Connect USB", "Connect", "USB Baglan", "USB Bağlan"}.Contains(e.Current.Name.Replace("&", ""))).ToArray();
        if (items.Length != 1) return false;
        object pattern;
        if (!items[0].TryGetCurrentPattern(InvokePattern.Pattern, out pattern)) return false;
        ((InvokePattern)pattern).Invoke(); return true;
    }
    protected override void ExitThreadCore() {
        timer.Stop(); timer.Dispose(); icon.Visible = false; icon.Dispose();
        NetworkChange.NetworkAddressChanged -= Changed; NetworkChange.NetworkAvailabilityChanged -= Availability;
        base.ExitThreadCore();
    }
    public static void Run() {
        bool created;
        using (var mutex = new Mutex(true, "Local\\PdaNetNobetci",out created)) {
            if (!created) return;
            Application.EnableVisualStyles(); Application.Run(new Nobetci());
        }
    }
}
