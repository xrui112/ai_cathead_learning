package cn.cathead.ai.domain.client.service.advisor.memory.manager.tools;

import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.service.IModelService;
import lombok.RequiredArgsConstructor;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.config.MemoryProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 动态策略提供器：根据模型上下文能力与配置生成压缩触发策略
 */
@Component
@RequiredArgsConstructor
public class ShortTermPolicyProvider {

    private final IModelService modelService;
    private final MemoryProperties props;

    /**
     * 根据 modelId/agentId 与模型域配置动态返回策略
     */
    public ShortTermPolicy getPolicy(String modelId, String agentId) {
        int windowTokens = resolveContextWindowTokens(modelId);
        return ShortTermPolicy.of(windowTokens, props.getStm().getCompressThresholdRatio(), props.getStm().getMaxMessages());
    }

    private int resolveContextWindowTokens(String modelId) {
        String useModelId = (modelId != null && !modelId.isBlank()) ? modelId : props.getStm().getDefaultModelId();
        if (useModelId == null || useModelId.isBlank()) {
            return props.getStm().getDefaultWindowTokens();
        }
        try {
            BaseModelEntity base = modelService.getModelById(useModelId);
            if (base instanceof ChatModelEntity chat) {
                Map<String, Object> props = chat.getDynamicProperties();
                if (props != null) {
                    Object v = firstNonNull(props.get("max_context_length"), props.get("context_window_tokens"), props.get("ctx_window"));
                    Integer parsed = tryParseInt(v);
                    if (parsed != null && parsed > 0) return parsed;
                }
                if (chat.getMaxTokens() != null && chat.getMaxTokens() > 0) {
                    return chat.getMaxTokens();
                }
            }
        } catch (Exception ignored) {
        }
        return props.getStm().getDefaultWindowTokens();
    }

    private static Object firstNonNull(Object... arr) {
        if (arr == null) return null;
        for (Object o : arr) if (o != null) return o;
        return null;
    }

    private static Integer tryParseInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }
}


