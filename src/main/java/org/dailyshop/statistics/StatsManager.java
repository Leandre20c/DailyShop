package org.dailyshop.statistics;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsManager {

    private final List<SaleRecord> sales = new ArrayList<>();
    private final File statsFile;
    private final Gson gson = new Gson();

    public StatsManager(File dataFolder) {
        this.statsFile = new File(dataFolder, "sales.json");
        load();
    }

    public void recordSale(String player, String item, int quantity, double price) {
        SaleRecord record = new SaleRecord(player, item, quantity, price, System.currentTimeMillis());
        sales.add(record);
        save();
    }

    public List<SaleRecord> getAllSales() {
        return sales;
    }

    public void exportToCSV(File file) {
        record SaleKey(String player, String itemId, double unitPrice) {}
        record AggregatedSale(long firstTimestamp, int totalAmount, double totalRevenue) {}

        Map<SaleKey, AggregatedSale> aggregated = new HashMap<>();

        for (SaleRecord record : sales) {
            String itemId = record.getItemId() != null ? record.getItemId() : "UNKNOWN";
            double unitPrice = record.getQuantity() > 0 ? record.getTotalPrice() / record.getQuantity() : 0.0;

            SaleKey key = new SaleKey(record.getPlayerName(), itemId, unitPrice);
            aggregated.merge(key,
                    new AggregatedSale(record.getTimestamp(), record.getQuantity(), record.getTotalPrice()),
                    (oldVal, newVal) -> new AggregatedSale(
                            Math.min(oldVal.firstTimestamp(), newVal.firstTimestamp()),
                            oldVal.totalAmount() + newVal.totalAmount(),
                            oldVal.totalRevenue() + newVal.totalRevenue()
                    ));
        }

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.println("timestamp,player,itemId,amount,total");

            for (Map.Entry<SaleKey, AggregatedSale> entry : aggregated.entrySet()) {
                SaleKey key = entry.getKey();
                AggregatedSale agg = entry.getValue();

                writer.printf(
                        "%d,%s,%s,%d,%.2f%n",
                        agg.firstTimestamp(),
                        key.player(),
                        key.itemId(),
                        agg.totalAmount(),
                        agg.totalRevenue()
                );
            }

        } catch (IOException e) {
            Bukkit.getLogger().severe("Erreur lors de l'export CSV des ventes : " + e.getMessage());
        }
    }


    private static class SaleStats {
        long timestamp;
        String player;
        String itemId;
        int amount;
        double total;

        public SaleStats(long timestamp, String player, String itemId, int amount, double total) {
            this.timestamp = timestamp;
            this.player = player;
            this.itemId = itemId;
            this.amount = amount;
            this.total = total;
        }
    }




    public void save() {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(statsFile), StandardCharsets.UTF_8)) {
            gson.toJson(sales, writer);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Erreur lors de la sauvegarde des statistiques : " + e.getMessage());
        }
    }

    public void load() {
        if (!statsFile.exists()) return;

        try (Reader reader = new InputStreamReader(new FileInputStream(statsFile), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<List<SaleRecord>>() {}.getType();
            List<SaleRecord> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                sales.addAll(loaded);
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("Erreur lors du chargement des statistiques : " + e.getMessage());
        }
    }

    public void clearAll() {
        sales.clear();

        // Écrase le fichier avec une liste vide
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(statsFile), StandardCharsets.UTF_8)) {
            gson.toJson(sales, writer);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Erreur lors de la suppression des statistiques : " + e.getMessage());
        }
    }
}
