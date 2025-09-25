package cn.cathead.ai.trigger.http;

import cn.cathead.ai.domain.client.service.advisor.memory.manager.IMemoryManager;
import cn.cathead.ai.types.model.Response;
import cn.cathead.ai.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/memory")
@Slf4j
public class MemoryController {

    @Resource
    private IMemoryManager memoryManager;

    @PostMapping("commit")
    public Response<String> commit(@RequestBody CommitMemoryRequest req) {
        try {
            memoryManager.saveLongTermText(
                    req.getSessionId(), req.getKnowledgeId(), req.getAgentId(),
                    req.getTitle(), req.getText(), req.getTags(), req.getImportance());
            return new Response<>(ResponseCode.SUCCESS.getCode(), "memory committed", null);
        } catch (Exception e) {
            log.error("commit memory failed: {}", e.getMessage(), e);
            return new Response<>(ResponseCode.FAILED.getCode(), e.getMessage(), null);
        }
    }

    @Data
    public static class CommitMemoryRequest {
        private String sessionId;
        private String knowledgeId;
        private String agentId;
        private String title;
        private String text;
        private List<String> tags;
        private Double importance;
    }
}


