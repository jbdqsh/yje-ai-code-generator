package com.yje.aicodegenerator.design;

import com.yje.aicodegenerator.exception.BusinessException;
import com.yje.aicodegenerator.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class GenerationRunRegistry {
    private static final ConcurrentHashMap<Long, Lease> RUNS = new ConcurrentHashMap<>();

    public Lease acquire(long appId) {
        Lease lease = new Lease();
        if (RUNS.putIfAbsent(appId, lease) != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该应用正在生成，请等待当前请求完成");
        }
        return lease;
    }

    public static Lease current(Object memoryId) {
        return memoryId instanceof Number id ? RUNS.get(id.longValue()) : null;
    }

    public void release(long appId, Lease lease) {
        lease.cancelled.set(true);
        RUNS.remove(appId, lease);
    }

    public static final class Lease {
        private final AtomicBoolean cancelled = new AtomicBoolean();

        public boolean isCancelled() {
            return cancelled.get();
        }
    }
}
