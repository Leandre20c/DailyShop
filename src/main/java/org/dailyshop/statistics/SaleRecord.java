package org.dailyshop.statistics;

public class SaleRecord {

    private final String playerName;
    private final String itemId;
    private final int quantity;
    private final double totalPrice;
    private final long timestamp;

    public SaleRecord(String playerName, String itemId, int quantity, double totalPrice, long timestamp) {
        this.playerName = playerName;
        this.itemId = itemId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.timestamp = timestamp;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
