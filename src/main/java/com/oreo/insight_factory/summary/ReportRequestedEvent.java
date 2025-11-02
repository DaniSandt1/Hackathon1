package com.oreo.insight_factory.summary;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

@Getter
public class ReportRequestedEvent extends ApplicationEvent {
    private final String branch;
    private final Instant from;
    private final Instant to;
    private final String emailTo;

    public ReportRequestedEvent(Object source, String branch, Instant from, Instant to, String emailTo) {
        super(source);
        this.branch = branch;
        this.from = from;
        this.to = to;
        this.emailTo = emailTo;
    }
}
