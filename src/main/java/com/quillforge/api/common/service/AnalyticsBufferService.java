package com.quillforge.api.common.service;

import java.util.UUID;

public interface AnalyticsBufferService {
    void bufferView(String type, UUID entityId, String referrer, String ipAddress, String userAgent, String search);
    void bufferReadProgress(String type, UUID entityId, int milestone);
    void flushMetrics();
}
