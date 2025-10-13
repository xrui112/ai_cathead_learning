package cn.cathead.ai.domain.client.service.mcp.tools;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 工具 Advisor 注册表：按 agentId 提供额外的 Advisors（工具/函数调用能力）。
 * 你可以在应用启动时或运行时把不同 Agent 的工具 Advisor 注册进来。
 */
@Component
public class McpAdvisorRegistry {

    private final Map<String, List<Advisor>> agentIdToAdvisors = new ConcurrentHashMap<>();

    public void register(String agentId, List<Advisor> advisors) {
        if (agentId == null || advisors == null) return;
        agentIdToAdvisors.put(agentId, List.copyOf(advisors));
    }

    public List<Advisor> resolveAdvisorsForAgent(String agentId) {
        if (agentId == null) return Collections.emptyList();
        return agentIdToAdvisors.getOrDefault(agentId, Collections.emptyList());
    }
}


