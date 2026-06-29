Hava Durumu Poster App
======================

Bu proje 384x1080 gibi dar ve dikey LED/poster ekranlarda okunaklı görünmesi için hazırlanmıştır.
Uygulama portre moduna kilitlidir, tam ekran çalışır ve hava durumunu internet üzerinden çeker.

Özellikler
----------
- Android native Java uygulaması
- Portre mod kilidi
- Tam ekran koyu poster tasarım
- Büyük sıcaklık yazısı
- Nem, rüzgar, yağış bilgisi
- 5 günlük tahmin
- Şehir adına dokununca yenileme
- Şehir adına uzun basınca şehir değiştirme
- Open-Meteo API kullanır, API anahtarı gerekmez
- Varsayılan şehir: İstanbul

APK alma
--------
1) Android Studio'yu aç.
2) File > Open ile bu klasörü seç: HavaDurumuPosterApp
3) Gradle Sync tamamlanınca Build > Build Bundle(s) / APK(s) > Build APK(s) seç.
4) APK genelde şu klasörde oluşur:
   app/build/outputs/apk/debug/app-debug.apk

Not
---
Bu ChatGPT ortamında Android SDK ve Gradle kurulu olmadığı için APK burada derlenemedi.
Proje dosyaları eksiksizdir; Android Studio ile açıldığında APK alınabilir.

Ekran oranı notu
----------------
384x1080 standart 9:16 değildir; 9:16 yaklaşık 607x1080 olur.
Bu uygulama özellikle 384x1080 dar-dikey poster ekranına göre güvenli boşluklarla tasarlandı.
