package com.example.finsaver.models;

import java.io.Serializable;
import java.util.Date;

public class Transactions implements Serializable {
    private int transactionId;
    private int userId;
    private double amount;
    private String description;
    private String type; // income/expense
    private String category;
    private Date date;

    public void setUserId(int userId){ this.userId = userId; }

    public int getUserId() { return userId; }

    public void setAmount(double amount) { this.amount = amount; }

    public double getAmount() { return amount; }

    public String getType() { return type; }

    public void setType(Object type) { this.type = type.toString(); }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }

    public void setCategory(String category) { this.category = category; }

    public void setDate(Date date) { this.date = date; }

    public Date getDate() { return date; }

    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public int getTransactionId() { return transactionId; }
}
