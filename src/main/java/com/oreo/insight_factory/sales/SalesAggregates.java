package com.oreo.insight_factory.sales;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SalesAggregates {
    private long totalUnits;
    private double totalRevenue;
    private String topSku;
    private String topBranch;
}
