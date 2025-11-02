package com.oreo.insight_factory.sales;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesAggregationServiceTest {

    @Mock
    private SalesRepository salesRepository;

    @InjectMocks
    private SalesAggregationService service;

    private Sale createSale(String sku, int units, double price, String branch) {
        Sale s = new Sale();
        s.setSku(sku);
        s.setUnits(units);
        s.setPrice(price);
        s.setBranch(branch);
        s.setSoldAt(Instant.now());
        return s;
    }

    @Test
    void shouldCalculateCorrectAggregatesWithValidData() {
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.99, "Miraflores"),
                createSale("OREO_DOUBLE", 5, 2.49, "San Isidro"),
                createSale("OREO_CLASSIC", 15, 1.99, "Miraflores")
        );
        when(salesRepository.findBySoldAtBetween(any(), any())).thenReturn(mockSales);

        SalesAggregates result = service.calculateAggregates(
                Instant.now().minusSeconds(6000), Instant.now(), null);

        assertThat(result.getTotalUnits()).isEqualTo(30);
        // 10*1.99 + 5*2.49 + 15*1.99 = 62.2
        assertThat(result.getTotalRevenue()).isCloseTo(62.2, within(0.01));
        assertThat(result.getTopSku()).isEqualTo("OREO_CLASSIC");
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
    }

    @Test
    void shouldHandleEmptyListGracefully() {
        when(salesRepository.findBySoldAtBetween(any(), any())).thenReturn(List.of());

        SalesAggregates result = service.calculateAggregates(
                Instant.now().minusSeconds(6000), Instant.now(), null);

        assertThat(result.getTotalUnits()).isZero();
        assertThat(result.getTotalRevenue()).isZero();
        assertThat(result.getTopSku()).isNull();
        assertThat(result.getTopBranch()).isNull();
    }

    @Test
    void shouldFilterByBranchCorrectly() {
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 2.0, "Miraflores"),
                createSale("OREO_CLASSIC", 20, 2.0, "Surco")
        );
        when(salesRepository.findBySoldAtBetween(any(), any())).thenReturn(mockSales);

        SalesAggregates result = service.calculateAggregates(
                Instant.now().minusSeconds(6000), Instant.now(), "Miraflores");

        assertThat(result.getTotalUnits()).isEqualTo(10);
        assertThat(result.getTotalRevenue()).isEqualTo(20.0);
        assertThat(result.getTopBranch()).isEqualTo("Miraflores");
    }

    @Test
    void shouldConsiderOnlySalesWithinDateRange() {
        Instant now = Instant.now();
        List<Sale> mockSales = List.of(
                createSale("OREO_MINI", 5, 2.0, "Miraflores"),
                createSale("OREO_CLASSIC", 5, 2.0, "Miraflores")
        );
        when(salesRepository.findBySoldAtBetween(any(), any())).thenReturn(mockSales);

        SalesAggregates result = service.calculateAggregates(
                now.minusSeconds(3600), now, null);

        assertThat(result.getTotalUnits()).isEqualTo(10);
        assertThat(result.getTotalRevenue()).isEqualTo(20.0);
    }

    @Test
    void shouldSelectCorrectTopSkuWhenTieExists() {
        List<Sale> mockSales = List.of(
                createSale("OREO_CLASSIC", 10, 1.5, "Miraflores"),
                createSale("OREO_DOUBLE", 10, 2.0, "Surco")
        );
        when(salesRepository.findBySoldAtBetween(any(), any())).thenReturn(mockSales);

        SalesAggregates result = service.calculateAggregates(
                Instant.now().minusSeconds(6000), Instant.now(), null);

        assertThat(result.getTotalUnits()).isEqualTo(20);
        assertThat(result.getTotalRevenue()).isCloseTo(35.0, within(0.01));
        assertThat(result.getTopSku()).isIn("OREO_CLASSIC", "OREO_DOUBLE"); // empate
    }
}
