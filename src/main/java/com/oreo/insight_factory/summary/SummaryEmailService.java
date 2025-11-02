package com.oreo.insight_factory.summary;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SummaryEmailService {

    private final JavaMailSender mailSender;
    private final SummaryService summaryService;

    public SummaryEmailService(JavaMailSender mailSender, SummaryService summaryService) {
        this.mailSender = mailSender;
        this.summaryService = summaryService;
    }

    // ✅ Escucha el evento y procesa en background
    @Async
    @EventListener
    public void handleReportRequest(ReportRequestedEvent event) {
        try {
            System.out.println("🧾 Procesando reporte asíncrono para branch: " + event.getBranch());
            List<SalesSummaryDTO> summaries = summaryService.getSummary(event.getFrom(), event.getTo());
            sendWeeklySummaryEmailTo(event.getEmailTo(), summaries, event.getBranch(), event.getFrom(), event.getTo());
        } catch (Exception e) {
            System.err.println("❌ Error procesando reporte asíncrono: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Envía el correo con HTML + gráfico
    public void sendWeeklySummaryEmailTo(String emailTo, List<SalesSummaryDTO> summaries,
                                         String branch, Instant start, Instant end)
            throws MessagingException, IOException {

        byte[] chartImage = createSalesChart(summaries);

        // ==== Cuerpo HTML ====
        StringBuilder rows = new StringBuilder();
        double totalGlobal = 0.0;

        for (SalesSummaryDTO s : summaries) {
            double subtotal = s.getTotalSales();
            totalGlobal += subtotal;
            rows.append(String.format("""
                <tr>
                    <td>%s</td>
                    <td style='text-align:center;'>%d</td>
                    <td style='text-align:right;'>S/ %.2f</td>
                </tr>
            """, s.getBranch(), s.getTotalUnits(), subtotal));
        }

        String html = """
<html>
<body style="font-family:'Segoe UI',Arial,sans-serif;background-color:#0071ce;padding:30px;">
  <div style="max-width:600px;margin:auto;background:#ffffff;border-radius:10px;
              box-shadow:0 2px 6px rgba(0,0,0,0.2);padding:25px;">
    <div style="text-align:center;">
      <img src="cid:oreoLogo" style="width:80px;height:80px;margin-bottom:10px;border-radius:50%%;">
    </div>
    <h2 style="color:#0071ce;text-align:center;">📊 Resumen Semanal de Ventas</h2>
    <p style="color:#666;text-align:center;margin-top:-10px;">
      <b>Del %s al %s</b>
    </p>
    <hr style="border:none;border-top:1px solid #eee;margin:20px 0;">
    <table style="width:100%%;border-collapse:collapse;">
      <thead style="background-color:#0071ce;color:white;">
        <tr>
          <th style="padding:10px;text-align:left;">Sucursal</th>
          <th style="padding:10px;text-align:center;">Unidades</th>
          <th style="padding:10px;text-align:right;">Total (S/)</th>
        </tr>
      </thead>
      <tbody>
        %s
      </tbody>
    </table>
    <p style="margin-top:20px;text-align:right;color:#333;">
      <b>Total global: S/ %.2f</b>
    </p>
    <img src="cid:salesChart" style="width:100%%;border-radius:10px;margin-top:15px;">
    <p style="font-size:12px;color:#999;text-align:center;margin-top:30px;">
      Oreo Insight Factory © 2025 — Reporte generado automáticamente.
    </p>
  </div>
</body>
</html>
""".formatted(
                start.toString().substring(0, 10),
                end.toString().substring(0, 10),
                rows, totalGlobal
        );

        // ==== Crear correo ====
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(emailTo);
        helper.setSubject("📨 Resumen semanal de ventas — " + branch);
        helper.setText(html, true);

        // Logo Oreo
        helper.addInline("oreoLogo",
                new FileSystemResource("src/main/resources/static/img/oreo.png"),
                "image/png");

        // Gráfico de ventas inline
        helper.addInline("salesChart",
                new ByteArrayResource(chartImage),
                "image/png");

        mailSender.send(message);
        System.out.println("📨 Correo semanal enviado correctamente a " + emailTo);
    }

    // ✅ Método auxiliar para generar gráfico
    private byte[] createSalesChart(List<SalesSummaryDTO> summaries) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (SalesSummaryDTO s : summaries) {
            dataset.addValue(s.getTotalSales(), "Ventas (S/)", s.getBranch());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Ventas por Sucursal",
                "Sucursal",
                "Ventas (S/)",
                dataset,
                PlotOrientation.VERTICAL,
                false, true, false
        );

        chart.setBackgroundPaint(java.awt.Color.WHITE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, 600, 300);
        return baos.toByteArray();
    }

    // 🔹 Método de compatibilidad (ya existente para tests o scheduler)
    public void sendWeeklySummaryEmail() throws MessagingException, IOException {
        Instant end = Instant.now();
        Instant start = end.minus(7, ChronoUnit.DAYS);
        List<SalesSummaryDTO> summaries = summaryService.getSummary(start, end);
        sendWeeklySummaryEmailTo("mmmmmmmggghh@gmail.com", summaries, "Global", start, end);
    }
}
