package com.oreo.insight_factory.summary;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SummaryScheduler {

    private final SummaryEmailService summaryEmailService;

    public SummaryScheduler(SummaryEmailService summaryEmailService) {
        this.summaryEmailService = summaryEmailService;
    }

    @Scheduled(cron = "0 0 9 * * MON", zone = "America/Lima")
    public void sendWeeklySummary() {
        try {
            summaryEmailService.sendWeeklySummaryEmail();
        } catch (Exception e) {
            System.err.println("❌ Error al enviar resumen semanal: " + e.getMessage());
        }
    }
}
