package com.oreo.insight_factory.summary;

public class SalesSummaryDTO {
    private String branch;
    private long totalUnits;
    private double totalSales;

    public SalesSummaryDTO(String branch, long totalUnits, double totalSales) {
        this.branch = branch;
        this.totalUnits = totalUnits;
        this.totalSales = totalSales;
    }

    public String getBranch() {
        return branch;
    }

    public long getTotalUnits() {
        return totalUnits;
    }

    public double getTotalSales() {
        return totalSales;
    }
}
