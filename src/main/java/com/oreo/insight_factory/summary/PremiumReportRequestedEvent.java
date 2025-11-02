package com.oreo.insight_factory.summary;

import com.oreo.insight_factory.summary.PremiumSummaryRequest;
import com.oreo.insight_factory.users.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PremiumReportRequestedEvent extends ApplicationEvent {
    private final PremiumSummaryRequest req;
    private final User requester;

    public PremiumReportRequestedEvent(Object source, PremiumSummaryRequest req, User requester) {
        super(source);
        this.req = req;
        this.requester = requester;
    }
}
