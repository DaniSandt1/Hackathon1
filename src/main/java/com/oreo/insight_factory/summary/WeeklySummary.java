package com.oreo.insight_factory.summary;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WeeklySummary {
    private long totalSales;
    private int totalUnits;
    private double totalRevenue;
}
