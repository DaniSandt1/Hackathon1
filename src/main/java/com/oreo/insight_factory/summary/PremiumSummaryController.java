package com.oreo.insight_factory.summary;

import com.oreo.insight_factory.security.JwtService;
import com.oreo.insight_factory.summary.AckResponse;
import com.oreo.insight_factory.summary.PremiumSummaryRequest;
import com.oreo.insight_factory.summary.PremiumReportRequestedEvent;
import com.oreo.insight_factory.users.User;
import com.oreo.insight_factory.users.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/sales/summary")
@RequiredArgsConstructor
public class PremiumSummaryController {

    private final ApplicationEventPublisher events;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @PostMapping("/weekly/premium")
    public ResponseEntity<AckResponse> requestPremium(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody PremiumSummaryRequest req) {

        String token = authHeader.replace("Bearer ", "");
        String username = jwtService.extractUsername(token);
        User requester = userRepository.findByUsername(username).orElseThrow();

        // Si es BRANCH, forzar a su sucursal
        if ("BRANCH".equalsIgnoreCase(requester.getRole().name())) {
            req.setBranch(requester.getBranch());
        }

        // Publicar evento asíncrono
        events.publishEvent(new PremiumReportRequestedEvent(this, req, requester));

        AckResponse ack = AckResponse.builder()
                .requestId("req_premium_" + UUID.randomUUID())
                .status("PROCESSING")
                .message("Su reporte premium está siendo generado. Incluirá gráficos y PDF adjunto.")
                .estimatedTime("60-90 segundos")
                .requestedAt(Instant.now())
                .features(List.of("HTML_FORMAT", "CHARTS", "PDF_ATTACHMENT"))
                .build();

        return ResponseEntity.accepted().body(ack);
    }
}
