package com.oreo.insight_factory.summary;

import com.oreo.insight_factory.sales.Sale;
import com.oreo.insight_factory.sales.SalesRepository;
import com.oreo.insight_factory.llm.LlmClient;
import com.oreo.insight_factory.llm.SummaryValidator;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PremiumReportProcessor {

    private final JavaMailSender mailSender;
    private final SalesRepository salesRepository;
    private final LlmClient llmClient;
    private final SummaryValidator summaryValidator;

    @Async
    @EventListener
    public void handlePremiumReport(PremiumReportRequestedEvent event) {
        try {
            PremiumSummaryRequest req = event.getReq();

            // 1) Rango de fechas
            LocalDate toDate = (req.getTo() == null || req.getTo().isBlank())
                    ? LocalDate.now(ZoneId.of("America/Lima"))
                    : LocalDate.parse(req.getTo());
            LocalDate fromDate = (req.getFrom() == null || req.getFrom().isBlank())
                    ? toDate.minusDays(6)
                    : LocalDate.parse(req.getFrom());

            Instant from = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to = toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1);

            // 2) Filtrar ventas
            List<Sale> all = salesRepository.findBySoldAtBetween(from, to);
            List<Sale> filtered = (req.getBranch() == null || req.getBranch().isBlank())
                    ? all
                    : all.stream().filter(s -> req.getBranch().equalsIgnoreCase(s.getBranch()))
                    .collect(Collectors.toList());

            // 3) Agregados
            long totalUnits = filtered.stream().mapToLong(Sale::getUnits).sum();
            double totalRevenue = filtered.stream().mapToDouble(s -> s.getUnits() * s.getPrice()).sum();

            String topBranch = filtered.stream()
                    .collect(Collectors.groupingBy(Sale::getBranch, Collectors.summingLong(Sale::getUnits)))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse(null);

            String topSku = filtered.stream()
                    .collect(Collectors.groupingBy(Sale::getSku, Collectors.summingLong(Sale::getUnits)))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse(null);

            // 4) QuickChart (gráficos de barras y pastel)
            String chartBar = createBarChart(filtered);
            String chartPie = createPieChart(filtered);

            // 5) Generar resumen con LLM
            String summaryText;
            try {
                String llm = llmClient.summarizeWeekly((int) totalUnits, totalRevenue, topSku, topBranch);
                summaryText = summaryValidator.isValid(llm)
                        ? llm
                        : summaryValidator.fallback((int) totalUnits, totalRevenue, topSku, topBranch);
            } catch (Exception ex) {
                summaryText = summaryValidator.fallback((int) totalUnits, totalRevenue, topSku, topBranch);
            }

            // 6) HTML completo con ambos gráficos
            String range = fromDate.format(DateTimeFormatter.ISO_DATE) + " a " + toDate.format(DateTimeFormatter.ISO_DATE);

            String html = """
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <style>
    body{font-family:Segoe UI,Arial,sans-serif;background:#f6f7fb;margin:0;padding:0}
    .wrap{max-width:720px;margin:0 auto;padding:24px}
    .card{background:#fff;border-radius:12px;box-shadow:0 2px 10px rgba(0,0,0,.06);padding:24px}
    .header{background:#6B46C1;color:#fff;border-radius:12px 12px 0 0;padding:20px}
    .metric{display:inline-block;padding:14px 18px;background:#f2f2ff;border-radius:10px;margin:6px 8px 0 0}
    .muted{color:#666}
  </style>
</head>
<body>
  <div class="wrap">
    <div class="card">
      <div class="header">
        <h2>🍪 Reporte Semanal Oreo — Premium</h2>
        <div>%s</div>
      </div>
      <p class="muted">Sucursal: <b>%s</b></p>
      <div>
        <div class="metric">Total Units: <b>%d</b></div>
        <div class="metric">Total Revenue: <b>S/ %.2f</b></div>
        <div class="metric">Top Branch: <b>%s</b></div>
        <div class="metric">Top SKU: <b>%s</b></div>
      </div>
      <h3 style='margin-top:16px'>Resumen:</h3>
      <p>%s</p>
      <h3 style='margin-top:18px'>Distribución de Ventas</h3>
      %s
      <h3 style='margin-top:18px'>Top SKUs</h3>
      %s
      <p class="muted" style="margin-top:16px">Oreo Insight Factory © %d</p>
    </div>
  </div>
</body>
</html>
""".formatted(
                    range,
                    (req.getBranch() == null || req.getBranch().isBlank()) ? "Todas" : req.getBranch(),
                    totalUnits, totalRevenue,
                    topBranch == null ? "-" : topBranch,
                    topSku == null ? "-" : topSku,
                    summaryText,
                    req.isIncludeCharts() ? "<img style='width:100%;margin-top:16px;border-radius:10px' src=\"" + chartBar + "\"/>" : "",
                    req.isIncludeCharts() ? "<img style='width:100%;margin-top:16px;border-radius:10px' src=\"" + chartPie + "\"/>" : "",
                    Year.now().getValue()
            );

            // 7) PDF adjunto
            byte[] pdf = req.isAttachPdf()
                    ? buildPdf(range, req.getBranch(), totalUnits, totalRevenue, topBranch, topSku, summaryText)
                    : null;

            // 8) Enviar correo
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, pdf != null);
            helper.setTo(req.getEmailTo());
            helper.setSubject("🍪 Reporte Premium — " + range);
            helper.setText(html, true);
            if (pdf != null)
                helper.addAttachment("reporte-premium.pdf", () -> new java.io.ByteArrayInputStream(pdf));

            mailSender.send(message);
            System.out.println("✅ Premium report enviado a " + req.getEmailTo());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // === Helpers ===

    private String createBarChart(List<Sale> filtered) throws Exception {
        String config = """
        {
          type: 'bar',
          data: {
            labels: %s,
            datasets: [{ label: 'Unidades por Sucursal', data: %s, backgroundColor:'#6B46C1' }]
          },
          options: { plugins: { legend: { display: false } } }
        }
        """.formatted(
                toJsonArray(labelsByBranch(filtered)),
                toJsonArray(unitsByBranch(filtered))
        );
        return "https://quickchart.io/chart?c=" + URLEncoder.encode(config, StandardCharsets.UTF_8);
    }

    private String createPieChart(List<Sale> filtered) throws Exception {
        Map<String, Long> bySku = filtered.stream()
                .collect(Collectors.groupingBy(Sale::getSku, Collectors.summingLong(Sale::getUnits)));
        String config = """
        {
          type: 'pie',
          data: {
            labels: %s,
            datasets: [{
              data: %s,
              backgroundColor: ['#6B46C1','#B794F4','#D6BCFA','#9F7AEA','#553C9A','#E9D8FD']
            }]
          }
        }
        """.formatted(
                toJsonArray(new ArrayList<>(bySku.keySet())),
                toJsonArray(new ArrayList<>(bySku.values()))
        );
        return "https://quickchart.io/chart?c=" + URLEncoder.encode(config, StandardCharsets.UTF_8);
    }

    private static List<String> labelsByBranch(List<Sale> list) {
        return list.stream().map(Sale::getBranch).filter(Objects::nonNull).distinct().sorted().toList();
    }

    private static List<Long> unitsByBranch(List<Sale> list) {
        Map<String, Long> map = list.stream()
                .collect(Collectors.groupingBy(Sale::getBranch, Collectors.summingLong(Sale::getUnits)));
        return labelsByBranch(list).stream().map(b -> map.getOrDefault(b, 0L)).toList();
    }

    private static String toJsonArray(List<?> xs) {
        return xs.stream()
                .map(x -> (x instanceof String) ? "\"" + x + "\"" : String.valueOf(x))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private byte[] buildPdf(String range, String branch, long totalUnits, double totalRevenue,
                            String topBranch, String topSku, String summary) throws Exception {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(50, 770);
                cs.showText("Reporte Premium — Oreo Insight Factory");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(50, 740);
                cs.showText("Rango: " + range);
                cs.newLineAtOffset(0, -18);
                cs.showText("Sucursal: " + (branch == null ? "Todas" : branch));
                cs.newLineAtOffset(0, -18);
                cs.showText("Total Units: " + totalUnits);
                cs.newLineAtOffset(0, -18);
                cs.showText(String.format("Total Revenue: S/ %.2f", totalRevenue));
                cs.newLineAtOffset(0, -18);
                cs.showText("Top Branch: " + (topBranch == null ? "-" : topBranch));
                cs.newLineAtOffset(0, -18);
                cs.showText("Top SKU: " + (topSku == null ? "-" : topSku));
                cs.newLineAtOffset(0, -24);
                cs.showText("Resumen:");
                cs.newLineAtOffset(0, -18);
                cs.showText(summary != null ? summary.substring(0, Math.min(summary.length(), 400)) : "-");
                cs.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
