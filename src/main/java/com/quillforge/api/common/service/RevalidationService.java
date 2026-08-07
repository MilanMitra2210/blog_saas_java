package com.quillforge.api.common.service;

public interface RevalidationService {
    void revalidate(String model, String slug, String action);
    void evictSpringCache(String model, String slug);
}
