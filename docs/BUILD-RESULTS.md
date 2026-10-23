# Yerel doğrulama — 23 Ekim 2026

* Android `assembleDebug lintDebug testDebugUnitTest`: **BUILD SUCCESSFUL** (Java 17, Gradle 8.13, SDK 35, ASCII geçici derleme klasörü).
* JUnit: **5 test, 0 başarısız, 0 hata**.
* Android lint: **0 hata, 41 uyarı**. Uyarılar çoğunlukla Türkçe metinlerin kaynak dosyasına taşınması, UI oluşturma ve lint'in stopService çağrılarındaki SAM yanlış pozitifleridir. Lint hataları bastırılmadı.
* APK: `tr.nobetci.pdanet`, 0.1.0, minSdk 28, targetSdk 35. `apksigner verify --verbose`: doğrulandı, APK Signature Scheme v2, bir test imzalayıcısı.
* Windows `.NET Framework 4.8` Release derlemesi: **0 hata, 0 uyarı**.
* Windows EXE `--self-test`: **çıkış kodu 0**. Canlı kurtarma çağrılmadı.
* Telefon kurulumu, erişilebilirlik ağacında tıklamalar, Samsung arka plan/kilit/açılış davranışı ve Windows PdaNet menü kurtarması: **donanımda test edilmedi**.

İlk yerel denemede Java 25 uyumsuzluğu, daha sonra Türkçe klasör yolundan kaynaklanan JUnit ClassNotFoundException oluştu. Java 17 ve ASCII klasörde yeniden derleme ile çözüldü. Son teslim APK'sı başarılı son derlemeden kopyalandı.

## GitHub Actions ve yayın

İlk yazma denemesi HTTP 403 hatasıyla engellendi. Kullanıcı yanıtından sonra tekrar denendi ve GitHub bağlantısı kaynakları başarıyla yükledi.

[Başarılı Actions çalışması](https://github.com/CanKaya4/pdanetnobetci/actions/runs/39952290948): Android derleme/lint/JUnit, Windows derleme/self-test, APK/Windows/test raporu artifacts ve release işi başarılı.

[v0.1.0 sürümü](https://github.com/CanKaya4/pdanetnobetci/releases/tag/v0.1.0) yayımlandı. GitHub'dan indirilen gerçek APK'nın imzası apksigner ile ayrıca doğrulandı. İlk Windows release ZIP'inde alt klasördeki EXE eksikti; paketleme recursive/flat ZIP ve EXE varlığı kontrolü ile düzeltildi. Eksik paket için düzeltme iş akışı aynı APK'yı koruyarak Windows ZIP'ini ve kontrol toplamlarını yeniler.

SDK kurulumundaki eski `tools` paketinin bulunamaması açık `platform-tools platforms;android-35 build-tools;35.0.0` listesiyle düzeltildi. Başarılı Actions derlemesi cihaz testinin yerine geçmez; donanım matrisi hâlâ doğrulama bekler.

Gelecekte yeni sürüm için (uygulama sürüm kodu/adını güncelledikten sonra):

```sh
git push -u origin main
git tag v0.1.1
git push origin v0.1.1
```

İlk komut yazma yetkili Git oturumu gerektirir. Etiket zaten varsa yeniden oluşturmayın. Etiket iş akışı APK ve Windows ZIP dosyasını GitHub Releases'e yükler. İş akışı depoya workflow dosyası yazma, sürüm işi ise Contents write erişimi gerektirir. Yerel APK ile CI çıktılarının test imzaları farklı olabilir.
