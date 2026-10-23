# Yerel doğrulama — 23 Ekim 2026

* Android `assembleDebug lintDebug testDebugUnitTest`: **BUILD SUCCESSFUL** (Java 17, Gradle 8.13, SDK 35, ASCII geçici derleme klasörü).
* JUnit: **5 test, 0 başarısız, 0 hata**.
* Android lint: **0 hata, 41 uyarı**. Uyarılar çoğunlukla Türkçe metinlerin kaynak dosyasına taşınması, UI oluşturma ve lint'in stopService çağrılarındaki SAM yanlış pozitifleridir. Lint hataları bastırılmadı.
* APK: `tr.nobetci.pdanet`, 0.1.0, minSdk 28, targetSdk 35. `apksigner verify --verbose`: doğrulandı, APK Signature Scheme v2, bir test imzalayıcısı.
* Windows `.NET Framework 4.8` Release derlemesi: **0 hata, 0 uyarı**.
* Windows EXE `--self-test`: **çıkış kodu 0**. Canlı kurtarma çağrılmadı.
* Telefon kurulumu, erişilebilirlik ağacında tıklamalar, Samsung arka plan/kilit/açılış davranışı ve Windows PdaNet menü kurtarması: **donanımda test edilmedi**.

İlk yerel denemede Java 25 uyumsuzluğu, daha sonra Türkçe klasör yolundan kaynaklanan JUnit ClassNotFoundException oluştu. Java 17 ve ASCII klasörde yeniden derleme ile çözüldü. Son teslim APK'sı başarılı son derlemeden kopyalandı.

## GitHub engeli

Depo boş olarak klonlandı. GitHub bağlantısı depo metadatasında push/admin izni gösterdi; fakat `README.md` oluşturma işlemi **HTTP 403 Resource not accessible by integration** hatasıyla reddedildi. Yerel Git Credential Manager'da GitHub hesabı bulunmadı; push kimlik doğrulaması yapamadı. Hiçbir kaynak dosyanın GitHub'a yüklendiği, Actions çalıştırıldığı veya Release yayımlandığı iddia edilmez.

Yerel kaynak ve wrapper hazırdır. Erişim düzelince kaynak klasöründe:

```sh
git push -u origin main
git tag v0.1.0
git push origin v0.1.0
```

İlk komut yazma yetkili Git oturumu gerektirir. Etiket zaten varsa yeniden oluşturmayın. Actions'ın açık olduğundan ve işlerin başarılı tamamlandığından emin olun. Etiket iş akışı APK ve Windows ZIP dosyasını GitHub Releases'e yükler. İş akışı depoya workflow dosyası yazma, sürüm işi ise Contents write erişimi gerektirir. Yerel APK ile CI çıktılarının test imzaları farklı olabilir.
