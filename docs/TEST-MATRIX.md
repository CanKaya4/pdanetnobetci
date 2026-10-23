# Test ve kabul durumu

Derleme/lint/JUnit sonuçları Actions çalıştırmasından doğrulanmalıdır. Politika testleri yabancı paket, satın alma düğmesi, izin metni, kısmi mesaj ve backoff sınırını kapsar. Windows self-test yalnızca C# derlemesi ve temel korumaları sınar. Emulator, fiziksel Samsung USB/PdaNet davranışını doğrulamaz. Bu çalışma sırasında mevcut internet bağlantısı bilerek kesilmedi, ADB sunucusu başlatılmadı (PdaNet ile çakışabilir).

| Senaryo | Beklenen davranış | Donanım durumu |
|---|---|---|
| 1 Normal bağlantı | Açık USB'ye tıklama yok; PC Up iken Connect yok | Yapılmadı |
| 2 Kesinti mesajı | Tam paket/mesaj/başlık/button2 eşleşmesinde Cancel | Statik pencere doğrulandı; canlı test yapılmadı |
| 3 USB kapanması | Bilinen UI'da kapalı checkbox açılmalı | Yapılmadı |
| 4 Ekran açık | Olaydan sonra hızlı kurtarma başlangıcı, süre logu | Yapılmadı |
| 5 Telefon kilitli | Tıklama yok, kilit nedeni görünmeli | Yapılmadı |
| 6 PdaNet arka planda | Görünür güvenilir pencere yoksa kurtarılamadığı belirtilmeli | Yapılmadı |
| 7 PdaNet zorla durduruldu | Uydurma başarı yok; elle açma gerekebilir | Yapılmadı |
| 8 PC istemcisi koptu | Adaptör Down doğrulandıktan sonra UIA Connect | Yapılmadı |
| 9 Telefon hazır, PC gelmiyor | Android WAIT_FOR_PC; Windows kontrollü tekrar | Yapılmadı |
| 10 Art arda kopma | Debounce, tek iş ve backoff; hazır olunca sıfırlama | Politika birim testi; bütünleşik test yapılmadı |
| 11 İzin kaldırıldı | Android koruması güvenle kapanmalı | Yapılmadı |
| 12 Uygulama yeniden açıldı | Kalıcı tercih; eski tıklama sürdürülmez | Yapılmadı |
| 13 Telefon yeniden başladı | Uygun koşullarda boot/servis; taze olay bekleme | Yapılmadı |

## Cihazda kabul testi

Önce PdaNet kimliğini ve sürümünü ana ekrandan doğrulayın. Gerçek süre dolumu sırasında tanılama ağacını inceleyin; bilinmeyen metinde hiçbir düğmeye basılmadığını kontrol edin. Süre dolumu → dialog → kapalı USB → açık USB → PC adaptörü Up zincirindeki saatleri telefon ve Windows loglarından karşılaştırın. Android telefon hazır süresi uçtan uca internet kurtarma süresi değildir. Ekran kilidi ve arka plan senaryolarını ayrı tekrarlayın. İzin kaldırma ve koruma kapatma sonrasında yeni tıklama olmadığını doğrulayın. Donanım testleri tamamlanmadan son kullanıcı kabul kriterlerinin tamamı sağlandı denemez.
