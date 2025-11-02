package com.oreo.insight_factory.summary;

import com.oreo.insight_factory.sales.Sale;
import com.oreo.insight_factory.sales.SalesRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SummaryService {

    private final SalesRepository salesRepository;

    public SummaryService(SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    public List<SalesSummaryDTO> getSummary(Instant start, Instant end) {
        // Busca todas las ventas dentro del rango semanal
        List<Sale> sales = salesRepository.findBySoldAtBetween(start, end);

        Map<String, List<Sale>> grouped = sales.stream()
                .collect(Collectors.groupingBy(Sale::getBranch));

        return grouped.entrySet().stream()
                .map(entry -> {
                    String branch = entry.getKey();
                    long totalUnits = entry.getValue().stream().mapToLong(Sale::getUnits).sum();
                    double totalSales = entry.getValue().stream()
                            .mapToDouble(s -> s.getUnits() * s.getPrice())
                            .sum();
                    return new SalesSummaryDTO(branch, totalUnits, totalSales);
                })
                .toList();
    }
}
