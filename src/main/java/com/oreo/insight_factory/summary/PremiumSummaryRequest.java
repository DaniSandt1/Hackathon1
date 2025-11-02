package com.oreo.insight_factory.summary;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PremiumSummaryRequest {
    // ISO yyyy-MM-dd (opcional). Si van null, se calcula última semana.
    private String from;
    private String to;

    // BRANCH obligatorio si el rol es BRANCH; CENTRAL puede enviar cualquiera o null (global)
    private String branch;

    @NotBlank @Email
    private String emailTo;

    // "PREMIUM" por consistencia; puedes permitir "BASIC"/"PREMIUM" si quieres
    private String format = "PREMIUM";

    private boolean includeCharts = true;
    private boolean attachPdf = true;
}
