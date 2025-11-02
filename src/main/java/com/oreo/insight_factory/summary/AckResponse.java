package com.oreo.insight_factory.summary;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data @Builder
public class AckResponse {
    private String requestId;
    private String status;        // PROCESSING
    private String message;
    private String estimatedTime; // "60-90 segundos"
    private Instant requestedAt;
    private List<String> features; // e.g. ["HTML_FORMAT","CHARTS","PDF_ATTACHMENT"]
}
