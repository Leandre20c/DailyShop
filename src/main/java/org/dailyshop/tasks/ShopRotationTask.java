package org.dailyshop.tasks;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.dailyshop.DailyShopPlugin;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class ShopRotationTask {

    private final DailyShopPlugin plugin;

    public ShopRotationTask(DailyShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void schedule() {
        var cfg = plugin.getConfig();
        String dailyTime = cfg.getString("daily-reset-time", "").trim();
        if (!dailyTime.isEmpty()) {
            // 1) Parse l'heure en LocalTime
            LocalTime resetTime = LocalTime.parse(dailyTime, DateTimeFormatter.ofPattern("HH:mm"));

            // 2) Calcule le délai jusqu'à la prochaine exécution
            long delayTicks = computeInitialDelayTicks(resetTime);

            // 3) Période = 24h en ticks (20 ticks = 1 s)
            long periodTicks = Duration.ofDays(1).getSeconds() * 20;

            // 4) Planifie la tâche
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                plugin.getShopManager().rotateShop();
                // Persiste la rotation dans la config
                plugin.getShopManager().saveRotationState(LocalDate.now());
            }, delayTicks, periodTicks);

            plugin.getLogger().info("Rotation programmée tous les jours à " + dailyTime +
                    " (première exécution dans " + (delayTicks / 20) + " s).");
        } else {
            // Fallback sur reset-every-hours si daily-reset-time est vide
            int hours = cfg.getInt("reset-every-hours", 24);
            long periodTicks = Duration.ofHours(hours).getSeconds() * 20;
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                plugin.getShopManager().rotateShop();
                plugin.getShopManager().saveRotationState(LocalDate.now());
            }, periodTicks, periodTicks);
            plugin.getLogger().info("Rotation programmée toutes les " + hours + " heures.");
        }
    }

    private long computeInitialDelayTicks(LocalTime resetTime) {
        ZoneId zone = ZoneId.systemDefault(); // Europe/Paris
        LocalDateTime now = LocalDateTime.now(zone);

        LocalDateTime nextRun = LocalDate.now(zone).atTime(resetTime);
        if (!now.isBefore(nextRun)) {
            // si on est déjà passé, on décale d'un jour
            nextRun = nextRun.plusDays(1);
        }

        Duration until = Duration.between(now, nextRun);
        return until.getSeconds() * 20;
    }
}
