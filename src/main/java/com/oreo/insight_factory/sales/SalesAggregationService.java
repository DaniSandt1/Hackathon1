package com.oreo.insight_factory.sales;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesAggregationService {

    private final SalesRepository salesRepository;

    public SalesAggregates calculateAggregates(Instant start, Instant end, String branchFilter) {
        List<Sale> allSales = salesRepository.findBySoldAtBetween(start, end);

        // Filtrado por sucursal (si se pasa)
        List<Sale> filtered = (branchFilter == null)
                ? allSales
                : allSales.stream()
                .filter(s -> s.getBranch().equalsIgnoreCase(branchFilter))
                .toList();

        long totalUnits = filtered.stream().mapToLong(Sale::getUnits).sum();
        double totalRevenue = filtered.stream().mapToDouble(s -> s.getUnits() * s.getPrice()).sum();

        // Agrupar por SKU y branch
        Map<String, Long> unitsBySku = filtered.stream()
                .collect(Collectors.groupingBy(Sale::getSku, Collectors.summingLong(Sale::getUnits)));
        Map<String, Long> unitsByBranch = filtered.stream()
                .collect(Collectors.groupingBy(Sale::getBranch, Collectors.summingLong(Sale::getUnits)));

        // Determinar top SKU y top branch
        String topSku = unitsBySku.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        String topBranch = unitsByBranch.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        return new SalesAggregates(totalUnits, totalRevenue, topSku, topBranch);
    }
}
