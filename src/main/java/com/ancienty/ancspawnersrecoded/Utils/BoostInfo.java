package com.ancienty.ancspawnersrecoded.Utils;

public class BoostInfo {

    private double amount;
    private long endTime;

    public BoostInfo(double amount, long endTime) {
        this.amount = amount;
        this.endTime = endTime;
    }

    public double getAmount() {
        return amount;
    }

    public long getEndTime() {
        return endTime;
    }
}

