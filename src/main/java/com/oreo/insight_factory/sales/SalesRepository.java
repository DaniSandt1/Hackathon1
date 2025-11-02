package com.oreo.insight_factory.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;

public interface SalesRepository extends JpaRepository<Sale, String> {

    @Query("SELECT s FROM Sale s WHERE (:branch IS NULL OR s.branch = :branch) " +
            "AND (:from IS NULL OR s.soldAt >= :from) " +
            "AND (:to IS NULL OR s.soldAt <= :to)")
    List<Sale> findByFilters(String branch, Instant from, Instant to);
    List<Sale> findBySoldAtBetween(Instant start, Instant end);
}
