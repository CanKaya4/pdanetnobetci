# PdaNet Nöbetçi

Kotlin Android uygulaması ve Windows için ücretsiz, yerel çalışan bağlantı yardımcısı. Root, VPN, reklam, telemetri, adaptör sıfırlama veya PdaNet yaması içermez. Android uygulamasının INTERNET izni yoktur.

**Durum: v0.1.0, donanım doğrulaması bekleyen ilk sürüm.** APK derlenmesi ile gerçek Galaxy A56 üzerinde otomatik kurtarmanın doğrulanması farklı kabul kriterleridir. Kesintisiz oyun bağlantısı garanti edilmez. USB el sıkışması sırasında mevcut oyun oturumu düşebilir.

## İndirme ve kurulum

GitHub Releases içindeki `PdaNet-Nobetci.apk` ve `Windows-Nobetci.zip` dosyalarını kullanın. Her push için Actions → Build and verify → Artifacts bölümünde de çıktılar bulunur. APK Android 9+ içindir; Galaxy A56'nın Android sürümünde kurulabilir hedef/min SDK kullanır, fakat cihaz üzerinde kurulum henüz test edilmedi. APK test anahtarıyla imzalanır; farklı CI çalıştırmalarının anahtarları değişebilir, güncelleme sırasında eski uygulamayı kaldırmak gerekebilir.

1. Telefona APK'yı kurun, uygulamadaki **İlk kurulum rehberi** üzerinden erişilebilirlik ve bildirim gösterme iznini verin. Bildirim erişimi isteğe bağlıdır. Samsung pil ayarlarından uygulamayı uykuya alınmayanlara ekleyin.
2. PdaNet'in normal USB bağlantısını telefon ve PC'de bir kez kurun, ardından **Korumayı aç** seçeneğini etkinleştirin. İlk çalıştırmada PdaNet'i görünür yapın. Kilit ekranını uygulama aşamaz.
3. Windows ZIP'ini kalıcı bir klasöre açıp EXE'yi çalıştırın. PdaNet tepsi simgesini her zaman görünür tutun. Nöbetçi tepsi menüsünden **Oturum açılışında başlat** seçeneğini seçin. Yönetici gerekmez.
4. İlk gerçek kesintide olay kayıtlarını inceleyin. Mesaj eşleşmezse Android **Tanılama ve mesaj deseni** ekranını kullanın. Yalnızca zararsız kesinti mesajını doğrulayın. PC menüsü UI Automation ile erişilebilir değilse yardımcı bunu kaydeder, kurtarmayı başarılı saymaz.

## Doğrulanmış PdaNet kimliği

Çalışma alanındaki orijinal `PdaNet5232.apk`, Windows PdaNet kurulumunun dağıttığı `PdaNet.apk` ile aynı SHA-256 değerine sahiptir:

`c6e421ad1294f969a9caf2c33804f0186eacd9631abfdefd785642b371690927`

Orijinal manifest: paket `com.pdanet`, sürüm adı `5.23`, sürüm kodu `5232`. Resmi indirme sayfasında pazarlama sürümü 5.23.2 olarak geçer. Telefonda kurulu sürüm ayrıca PackageManager ile gösterilir. Başka sürümlerde kullanıcı tanılama profilini onaylamadan otomasyon başlamaz. APK veya PdaNet kaynakları bu depoda dağıtılmaz ve değiştirilmez.

Statik incelemede `com.foxfi.HotspotSettings`, `pref_usb_on` / `USB Tether` tercihi ve şu pencere doğrulandı:

* Mesaj: `Usage limit reached in free mode. Please unlock the full version or restart free mode.`
* Başlık: `PdaNet+ has turned off` (çalışma anındaki uygulama adına bağlı; tanılamadan doğrulayın).
* Negatif düğme: `Cancel`, Android `android:id/button2`.
* Pozitif satın alma düğmesine asla tıklanmaz. Mesaj, başlık, düğme kimliği ve metni tam eşleşmelidir. Android izin pencereleri farklı paket olduklarından kapsam dışıdır.

Bu pencerenin ilgili sürümde varlığı statik olarak doğrulanmıştır; Galaxy A56'daki gerçek erişilebilirlik ağacı ve USB süre dolumu akışı henüz doğrulanmamıştır. Türkçe tam metinler bilinmediğinden uydurulmuş mesaj deseni yoktur; kullanıcı profil alanlarından ekleyebilir.

## Algılama ve kurtarma

Erişilebilirlik olayları sadece `com.pdanet` için alınır, 50 ms event birleştirmesi kullanılır. Tam eşleşen mesaj negatif düğmeyle kapatılır. Bilinen aktivitede `USB Tether` etiketli, tek açık/kapalı kontrol içeren tercih satırı bulunursa yalnızca **kapalı** kontrol açılır. Sabit koordinat kullanılmaz. Yeni kullanıcı arayüzünde belirsiz satırlar reddedilir.

Telefon kutusu açıldığında `WAIT_FOR_PC`, telefon hazır zamanı ve başarılı telefon kurtarma sayısı kaydedilir. Bu işlem uçtan uca başarı sayılmaz. Telefonun ConnectivityManager olayları PC'nin internetini ölçmez; bunlar yalnızca tanılamadır. Bildirim kaldırılması tek başına kopma kabul edilmez. Android ekranında PC başarısının bilinmediği açıkça gösterilir; Windows adaptör başarılarını kendi günlüğünde tutar.

Denemeler 750 ms tıklama aralığıyla sınırlandırılır, 10 saniyede doğrulanamayan işlem başarısız olur; bekleme 2/4/8/.../60 saniyedir. Yeni erişilebilirlik olayı veya kontrollü tekrar taze pencere ağacını okur. Telefon hazır doğrulanınca deneme sayacı sıfırlanır. Süreler loglanır; alt saniyede başlangıç hedefi donanımda ölçülmedi.

Telefon kilitliyken UI otomasyonu yapılmaz. PdaNet görünür değilken USB kutusu okunamaz. Servis PdaNet'i arka plandan zorla öne getirmez; manuel deneme kullanıcı eylemiyle açmayı dener. PdaNet'in süre dolumu penceresi arka planda görünür hale gelmezse kurtarma için kullanıcı gerekebilir. Bu sürüm bu koşullarda tam otonom kurtarma kabul kriterini henüz kanıtlamaz.

Koruma durumu ve son olaylar SharedPreferences içinde saklanır. Yeniden açılışta eski tıklama işleri sürdürülmez, taze UI kanıtı beklenir. En fazla 200 olay ve tek 24 KB tanılama ağacı tutulur. Erişilebilirlik izni kaldırılınca koruma kapatılır. Bildirimde durdurma düğmesi bulunur. Wakelock yoktur. Uygulamayı zorla durdurma Android açılış alıcılarını da engelleyebilir; yeniden elle açılması gerekir.

## Derleme

Java 17, Android SDK 35 ve Gradle wrapper:

Windows'ta Gradle/JUnit yol kodlaması nedeniyle projeyi yalnızca ASCII karakterli bir klasörde (ör. `C:\dev\pdanetnobetci`) derleyin. Türkçe klasör yolu test sınıflarının yüklenmesini engelleyebilir.

```sh
./gradlew assembleDebug lintDebug testDebugUnitTest
dotnet build windows/Nobetci.csproj -c Release
```

Windows EXE .NET Framework 4.8 kullanır. CI Android derleme/lint/JUnit ve Windows derleme/self-test işlemlerini çalıştırır, artifact yükler. `v*` etiketi başarılı iki işten sonra GitHub Release ve SHA256SUMS üretir. CI test imzası üretim için kalıcı imza stratejisi değildir.

## Belgeler

* [Test matrisi](docs/TEST-MATRIX.md)
* [Windows kullanım ve sınırlar](windows/README.md)

## Başvurular

* [Resmi PdaNet indirmeleri](https://pdanet.co/install/)
* [Resmi USB yardım sayfası](https://pdanet.co/help/usb.php): tepsi menüsünden Connect yöntemi belgelenir. Belgelenmiş yeniden bağlantı CLI'si bulunamadı; komut satırı parametresi varsayılmadı.
* [Android foreground service türleri](https://developer.android.com/develop/background-work/services/fgs/service-types): kullanıcı başlatmalı gözlem servisi `specialUse` kullanır; Play Store onayı iddia edilmez.

MIT lisansı. PdaNet ile resmi bağlantısı olmayan bağımsız yardımcıdır.
