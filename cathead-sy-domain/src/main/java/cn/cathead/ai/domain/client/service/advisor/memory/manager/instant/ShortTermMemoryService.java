package cn.cathead.ai.domain.client.service.advisor.memory.manager.instant;

import cn.cathead.ai.domain.client.model.entity.MemoryMessage;
import cn.cathead.ai.domain.client.model.valobj.NamespaceKey;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.instant.compress.IMemoryCompressor;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.tools.ShortTermPolicy;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.config.MemoryProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Primary
@Slf4j
@RequiredArgsConstructor
public class ShortTermMemoryService implements IShortTermMemoryService {

    private Cache<NamespaceKey, List<MemoryMessage>> buffer;
    private final ConcurrentMap<NamespaceKey, Double> tokenCounter = new ConcurrentHashMap<>();
    private final ConcurrentMap<NamespaceKey, Long> lastAccess = new ConcurrentHashMap<>();

    private final ExecutorService pool = Executors.newSingleThreadExecutor(new CustomizableThreadFactory("stm-compress-"));

    private final MemoryProperties props;

    @PostConstruct
    public void init() {
        this.buffer = Caffeine.newBuilder()
                .maximumSize(props.getStm().getMaxNamespaces())
                .expireAfterAccess(Duration.ofMinutes(props.getStm().getTtlMinutes()))
                .removalListener((NamespaceKey key, List<MemoryMessage> value, RemovalCause cause) -> {
                    if (key != null) {
                        tokenCounter.remove(key);
                        lastAccess.remove(key);
                    }
                })
                .build();
    }

    @Override
    public List<MemoryMessage> get(NamespaceKey namespaceKey) {
        List<MemoryMessage> list = buffer.getIfPresent(namespaceKey);
        if (list != null) lastAccess.put(namespaceKey, System.currentTimeMillis());
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    @Override
    public void append(NamespaceKey namespaceKey, List<MemoryMessage> messages) {
        buffer.asMap().merge(namespaceKey, new ArrayList<>(messages), (oldList, newList) -> {
            if (oldList == null) oldList = new ArrayList<>();
            oldList.addAll(newList);
            return oldList;
        });
        double sum = messages.stream().mapToDouble(MemoryMessage::getTokenCost).sum();
        tokenCounter.merge(namespaceKey, sum, Double::sum);
        lastAccess.put(namespaceKey, System.currentTimeMillis());
    }

    @Override
    public void replace(NamespaceKey namespaceKey, List<MemoryMessage> messages) {
        buffer.put(namespaceKey, new ArrayList<>(messages));
        double sum = messages.stream().mapToDouble(MemoryMessage::getTokenCost).sum();
        tokenCounter.put(namespaceKey, sum);
        lastAccess.put(namespaceKey, System.currentTimeMillis());
    }

    @Override
    public void clear(NamespaceKey namespaceKey) {
        buffer.invalidate(namespaceKey);
        tokenCounter.remove(namespaceKey);
        lastAccess.remove(namespaceKey);
    }

    @Override
    public int size(NamespaceKey namespaceKey) {
        List<MemoryMessage> list = buffer.getIfPresent(namespaceKey);
        return list == null ? 0 : list.size();
    }

    @Override
    public double totalEstimatedTokens(NamespaceKey namespaceKey) {
        return tokenCounter.getOrDefault(namespaceKey, 0.0);
    }

    @Override
    public List<NamespaceKey> keys() {
        return new ArrayList<>(buffer.asMap().keySet());
    }

    @Override
    public long lastAccessEpochMs(NamespaceKey namespaceKey) {
        return lastAccess.getOrDefault(namespaceKey, 0L);
    }

    @Override
    public void compressNamespaceAsync(NamespaceKey namespace, ShortTermPolicy policy, IMemoryCompressor memoryCompressor) {
        pool.submit(() -> {
            try {
                List<MemoryMessage> current = get(namespace);
                if (current == null || current.isEmpty()) return;
                double tokens = totalEstimatedTokens(namespace);
                int count = size(namespace);
                if (!policy.shouldCompress(tokens, count)) return;
                var cr = memoryCompressor.compress(current, policy);
                replace(namespace, cr.getRetainedMessages());
                log.info("[STM] namespace={} compressed ratio={}", namespace, cr.getCompressionRatio());
            } catch (Exception e) {
                log.warn("[STM] namespace={} error:{}", namespace, e.getMessage());
            }
        });
    }

    @Override
    public void compressAllLruFirst(ShortTermPolicy policy, IMemoryCompressor memoryCompressor) {
        List<NamespaceKey> nsKeys = keys();
        nsKeys.sort(Comparator.comparingLong(this::lastAccessEpochMs));
        for (NamespaceKey ns : nsKeys) {
            compressNamespaceAsync(ns, policy, memoryCompressor);
        }
    }

    public void shutdown() {
        pool.shutdown();
        try {
            pool.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
    }
}
