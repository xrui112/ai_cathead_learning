package cn.cathead.ai.domain.client.service.advisor.memory.manager.instant;

import cn.cathead.ai.domain.client.model.entity.MemoryMessage;
import cn.cathead.ai.domain.client.model.valobj.NamespaceKey;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.instant.compress.IMemoryCompressor;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.tools.ShortTermPolicy;

import java.util.List;

public interface IShortTermMemoryService {

    List<MemoryMessage> get(NamespaceKey namespaceKey);

    void append(NamespaceKey namespaceKey, List<MemoryMessage> messages);

    void replace(NamespaceKey namespaceKey, List<MemoryMessage> messages);

    void clear(NamespaceKey namespaceKey);

    int size(NamespaceKey namespaceKey);

    double totalEstimatedTokens(NamespaceKey namespaceKey);

    List<NamespaceKey> keys();

    long lastAccessEpochMs(NamespaceKey namespaceKey);

    void compressNamespaceAsync(NamespaceKey namespace, ShortTermPolicy policy, IMemoryCompressor memoryCompressor);

    void compressAllLruFirst(ShortTermPolicy policy, IMemoryCompressor memoryCompressor);
}


