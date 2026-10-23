# Windows yardımcısı

`PdaNet-Nobetci.exe` dosyasını kalıcı klasörden normal kullanıcı olarak çalıştırın. .NET Framework 4.8 gerekir (Windows 10/11'de genellikle bulunur). Tepsi simgesinin menüsünden otomatik başlatmayı ekleyebilir veya kaldırabilirsiniz. Kaldırmak için önce otomatik başlatmayı kaldırın, sonra çıkıp klasörü silin. Bu işlem HKCU Run içinde yalnızca PdaNetNobetci değerini yönetir.

Yalnızca adı `PdaNet Broadband Connection`, açıklaması PdaNet içeren tekil adaptör izlenir. NetworkChange olayları işaretlenir ve 250 ms UI zamanlayıcısında işlenir; normal durumda ağ listesi sürekli sorgulanmaz. Kopma 750 ms sonra tekrar doğrulanır. Adaptör Up iken bağlantı işlemi yapılmaz. Arızada en fazla 60 saniyeye çıkan beklemelerle yeniden denenir. Up durumu internet erişimini veya MyPublicWiFi istemcilerini doğrulamaz.

PdaNetPC işleminin erişilebilir Connect/Connect USB menüsü UI Automation InvokePattern ile kullanılır. Menü görünür değilse Explorer'a ait tekil görünür PdaNet tepsi simgesi odaklanır ve standart Shift+F10 menü tuşu gönderilir. Sürüm bunu desteklemiyorsa veya menü başka bir işlem tarafından sahipleniliyorsa işlem başarısız olarak kaydedilir. Koordinat, adaptör reseti, DNS değişikliği, sürücü kurulumu, süreç öldürme veya belgelenmemiş CLI kullanılmaz. Arıza sırasında menü odaklanması oyunun odağını etkileyebilir.

**Gerçek PdaNet Windows menüsünde otomatik kurtarma henüz test edilmedi.** Self-test derlenebilirlik, backoff ve Up/Down korumasını sınar; canlı ağ/UI kurtarma testi değildir. Simgeyi görünür tutmak ve istemciyi normal kullanıcı oturumunda çalıştırmak gerekir. Kilitli Windows oturumunda UIA başarısız olabilir.

Kayıt: `%LOCALAPPDATA%\PdaNetNobetci\windows.log` (256 KB rotasyon). Burada deneme, başarısızlık nedeni, adaptör yeniden bağlantı süresi ve başarı sayısı bulunur. Telefonla ağ üzerinden haberleşme yoktur; telefon hazır durumunu ancak PdaNet istemcisinin bağlantı denemesi sınar. Bu nedenle hazır olmayan telefon karşısında de kademeli bekleme uygulanır.

Kaynak derleme: `dotnet build windows/Nobetci.csproj -c Release`. Test: EXE'yi `--self-test` ile başlatıp çıkış kodunun 0 olduğunu kontrol edin. Otomatik başlangıç komutları: `--install-startup`, `--remove-startup`.
