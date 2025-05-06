package com.example.finsaver.models;

public class FamilyGroup {
    private int userId;
    private int groupId;
    private String username;
    private String fullName;
    private double totalIncome;
    private double totalExpenses;
    private boolean isCreator;

    public FamilyGroup(int userId, String username, String fullName, double totalIncome, double totalExpenses, boolean isCreator) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        this.isCreator = isCreator;
    }

    // Геттеры
    public String getName() {return fullName != null && !fullName.isEmpty() ? fullName : username; }

    public double getBalance() {return totalIncome - totalExpenses; }

    public double getTotalIncome() {return totalIncome; }

    public double getTotalExpenses() {return totalExpenses; }

    public int getUserId() { return userId;
    }

    public void setUserId(int userId) { this.userId = userId;
    }

    public int getGroupId() { return groupId; }

    public void setGroupId(int groupId) { this.groupId = groupId; }

    public boolean isCreator () { return  isCreator; }
}
