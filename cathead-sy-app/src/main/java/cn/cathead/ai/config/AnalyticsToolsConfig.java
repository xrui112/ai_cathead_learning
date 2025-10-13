package cn.cathead.ai.config;

import cn.cathead.ai.domain.exec.service.tools.AnalyticsTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;


@Slf4j
@Configuration
public class AnalyticsToolsConfig {


    @Bean
    public List<ToolCallback> analyticsToolCallbacks(AnalyticsTools analyticsTools) {
        log.info("=== 开始注册 AnalyticsTools 工具 ===");

        // 在方法内部创建 Provider，不注册为 Bean
        ToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                .toolObjects(analyticsTools)
                .build();

        // 从 Provider 获取所有 ToolCallback
        ToolCallback[] callbacks = provider.getToolCallbacks();

        log.info("✅ 成功注册 {} 个工具回调", callbacks.length);
        for (int i = 0; i < callbacks.length; i++) {
            ToolCallback callback = callbacks[i];
            // 尝试从 toString 中提取工具名称
            String callbackStr = callback.toString();
            String toolName = "未知";
            if (callbackStr.contains("name=")) {
                int start = callbackStr.indexOf("name=") + 5;
                int end = callbackStr.indexOf(",", start);
                if (end < 0) end = callbackStr.indexOf("]", start);
                if (end > start) {
                    toolName = callbackStr.substring(start, end).trim();
                }
            }
            log.info("  ✓ 工具 #{}: {}", i + 1, toolName);
        }

        return Arrays.asList(callbacks);
    }
}
