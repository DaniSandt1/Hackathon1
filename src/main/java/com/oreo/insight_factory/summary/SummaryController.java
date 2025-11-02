package com.oreo.insight_factory.summary;

import com.oreo.insight_factory.security.JwtService;
import com.oreo.insight_factory.users.Role;
import com.oreo.insight_factory.users.User;
import com.oreo.insight_factory.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

@RestController
@RequestMapping("/sales/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final ApplicationEventPublisher eventPublisher;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @PostMapping("/weekly")
    public ResponseEntity<Map<String, Object>> requestWeeklySummary(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body
    ) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtService.extractUsername(token);
        User user = userRepository.findByUsername(username).orElseThrow();

        String branch = body.get("branch");
        String emailTo = body.get("emailTo");
        String fromStr = body.get("from");
        String toStr = body.get("to");

        if (emailTo == null || emailTo.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "emailTo es obligatorio"));

        // Calcular semana pasada si no se envía rango
        Instant from = (fromStr != null)
                ? LocalDate.parse(fromStr).atStartOfDay().toInstant(ZoneOffset.UTC)
                : Instant.now().minusSeconds(7 * 24 * 3600);
        Instant to = (toStr != null)
                ? LocalDate.parse(toStr).atStartOfDay().toInstant(ZoneOffset.UTC)
                : Instant.now();

        // Si es BRANCH, limitar branch a su propia sucursal
        if (user.getRole() == Role.BRANCH) {
            branch = user.getBranch();
        }

        // Publicar evento asíncrono
        eventPublisher.publishEvent(new ReportRequestedEvent(this, branch, from, to, emailTo));

        return ResponseEntity.accepted().body(Map.of(
                "requestId", "req_" + System.currentTimeMillis(),
                "status", "PROCESSING",
                "message", "Su solicitud de reporte está siendo procesada. Recibirá el resumen en " + emailTo + " en unos momentos.",
                "estimatedTime", "30-60 segundos",
                "requestedAt", Instant.now().toString()
        ));
    }
}
