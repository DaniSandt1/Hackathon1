package com.oreo.insight_factory.llm;

import org.springframework.stereotype.Component;

@Component
public class SummaryValidator {
    public boolean isValid(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        if (trimmed.isBlank()) return false;

        int words = trimmed.split("\\s+").length;
        if (words > 120) return false;

        String t = trimmed.toLowerCase();
        boolean mentionsUnits   = t.contains("unidades") || t.contains("total units") || t.matches(".*\\b\\d+\\s*unid.*");
        boolean mentionsRevenue = t.contains("ingreso") || t.contains("recaud") || t.contains("revenue") || t.contains("s/") || t.contains("$");
        boolean mentionsTopSku  = t.contains("sku") || t.contains("producto más vendido") || t.contains("top sku");
        boolean mentionsBranch  = t.contains("sucursal") || t.contains("filial") || t.contains("branch");

        return mentionsUnits || mentionsRevenue || mentionsTopSku || mentionsBranch;
    }

    /** Fallback superseguro si el LLM devuelve algo inválido. */
    public String fallback(int totalUnits, double totalRevenue, String topSku, String topBranch) {
        return "Resumen semanal: Se vendieron " + totalUnits + " unidades por un total de S/ "
                + String.format("%.2f", totalRevenue) + ". "
                + "SKU destacado: " + (topSku == null ? "-" : topSku) + ". "
                + "Sucursal líder: " + (topBranch == null ? "-" : topBranch) + ".";
    }
}
