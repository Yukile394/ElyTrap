# 🪤 ElyTrap — Elytra Trap Mod

**Minecraft 1.21.x | Fabric | Client-Side**

ElyTrap, elytra ile uçan oyuncuları havada tuzağa düşüren bir PvP client modudur.

---

## ✨ Özellikler

### 🎯 Trap Modu (V Tuşu)
- En yakın oyuncuyu otomatik hedef alır
- Hedef **fişek kullandıkça** → o kadar knockback spam atar
- 1 fişek = 10 tick saldırı dalgası (config'den ayarlanabilir)
- Hedef belirli bir mesafeyi geçmeye çalışırsa geri itilir
- Trap kapanana kadar kesintisiz çalışır

### 🚀 Kaçış Modu (B Tuşu)
- Biz trapa düştüğümüzde aktif edilir
- Elytra ile bakış yönünde otomatik fırlatır
- 100 tik sonra otomatik kapanır (~5 saniye)

---

## ⌨️ Tuşlar

| Tuş | Fonksiyon |
|-----|-----------|
| **V** | Trap Aç/Kapat |
| **B** | Kaçış Modu Aç/Kapat |

> Tuşlar Minecraft ayarlarından değiştirilebilir: **Ayarlar → Kontroller → ElyTrap**

---

## ⚙️ Config Ayarları

`ElyTrapConfig.java` dosyasından:

```java
knockbackStrength = 3.5;        // Knockback kuvveti
attackCooldownTicks = 4;        // Saldırı hızı (tick, 4 = saniyede 5 vuruş)
trapBoundaryRadius = 20.0;      // Traptan çıkma engel yarıçapı (blok)
escapePushStrength = 2.0;       // Kaçış modu itme kuvveti
targetScanRadius = 30.0;        // Hedef arama yarıçapı
fireworkKnockbackTicks = 10;    // Fişek başına knockback tick sayısı
debugMode = false;              // Debug mesajları
```

---

## 🔨 Build

### Gereksinimler
- Java 21+
- Git

### Manuel Build
```bash
git clone https://github.com/KullaniciAdi/ElyTrap.git
cd ElyTrap
chmod +x gradlew
./gradlew build
```
JAR → `build/libs/elytrap-1.0.0.jar`

### GitHub Actions
`main` branch'e her push'ta otomatik build alınır.  
Release oluşturulduğunda JAR otomatik release'e yüklenir.

---

## 📁 Dosya Yapısı

```
ElyTrap/
├── .github/
│   └── workflows/
│       └── build.yml                    ← CI/CD otomatik build
├── src/main/
│   ├── java/com/elytrap/
│   │   ├── ElyTrapClient.java           ← Ana entrypoint, tuş atamaları
│   │   ├── config/
│   │   │   └── ElyTrapConfig.java       ← Tüm ayarlar burada
│   │   ├── handler/
│   │   │   └── TrapHandler.java         ← Ana mantık (trap + kaçış)
│   │   └── mixin/
│   │       └── FireworkRocketEntityMixin.java  ← Fişek algılama
│   └── resources/
│       ├── fabric.mod.json              ← Mod meta bilgisi
│       ├── elytrap.mixins.json          ← Mixin listesi
│       └── assets/elytrap/lang/
│           └── tr_tr.json               ← Türkçe dil dosyası
├── build.gradle
├── gradle.properties
├── settings.gradle
└── README.md
```

---

## ⚠️ Uyarı

Bu mod **yalnızca kendi sunucularında veya izin alınmış ortamlarda** kullanılmalıdır.  
Başkasının sunucusunda kullanmak kural ihlali sayılabilir.

---

## 📝 Lisans
MIT License
