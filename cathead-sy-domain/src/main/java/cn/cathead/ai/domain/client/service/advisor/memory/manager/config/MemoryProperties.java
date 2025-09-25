package cn.cathead.ai.domain.client.service.advisor.memory.manager.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.memory")
@Getter
@Setter
public class MemoryProperties {

    private final Stm stm = new Stm();
    private final Compression compression = new Compression();
    private final Ltm ltm = new Ltm();

    @Getter
    @Setter
    public static class Stm {
        private int maxNamespaces = 1000;
        private long ttlMinutes = 120;
        private int defaultWindowTokens = 8192;
        private double compressThresholdRatio = 0.85;
        private int maxMessages = 200;
        private String defaultModelId = "7c5d376d-3bf6-41dd-a5dc-a7390ae09a18";
    }

    @Getter
    @Setter
    public static class Compression {
        private String modelId = "7c5d376d-3bf6-41dd-a5dc-a7390ae09a18";
    }

    @Getter
    @Setter
    public static class Ltm {
        private String defaultEmbeddingModelId = "a0ab2e45-948e-46ec-95b2-7c7da0281daf";
        private int chunkSize = 1000;
        private int overlap = 100;
        private int defaultTopK = 5;
    }
}


