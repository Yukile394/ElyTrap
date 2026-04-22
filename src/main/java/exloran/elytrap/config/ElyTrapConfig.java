package com.elytrap.config;

/**
 * ElyTrap Configuration
 * Mod ayarlarını tutan basit config sınıfı.
 */
public class ElyTrapConfig {

    // Knockback kuvveti - ne kadar yüksek o kadar güçlü
    public static double knockbackStrength = 3.5;

    // Saniyede kaç kez saldırı vuracak (TPS bazlı tick)
    public static int attackCooldownTicks = 4; // 4 tick = 0.2 saniye = saniyede 5 vuruş

    // Traptan çıkmayı engellemek için sınır mesafesi (blok)
    public static double trapBoundaryRadius = 20.0;

    // Kaçış modunda kendini fırlatma kuvveti
    public static double escapePushStrength = 2.0;

    // Hedef oyuncunun etrafında arama yarıçapı
    public static double targetScanRadius = 30.0;

    // Debug mesajları göster
    public static boolean debugMode = false;

    // Trap aktif olduğunda otomatik uç
    public static boolean autoGlide = true;

    // Fişek başına kaç knockback tik uygulanacak
    public static int fireworkKnockbackTicks = 10; // her fişek = 10 tick knockback spam
}

