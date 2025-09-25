package cn.cathead.ai.domain.client.service.advisor.memory.manager.instant.compress;

import cn.cathead.ai.domain.client.model.entity.MemoryChunk;
import cn.cathead.ai.domain.client.model.entity.MemoryMessage;
import cn.cathead.ai.domain.client.model.valobj.CompressionResult;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.tools.MessageUtils;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.tools.ShortTermPolicy;
import cn.cathead.ai.domain.model.service.IModelService;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.config.MemoryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 语义压缩器：
 * - 构造结构化提示词，将旧消息提交给专用压缩模型，返回结构化摘要
 * - 保留近期若干条消息作为短期记忆
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SimpleMemoryCompressor implements IMemoryCompressor {

    private final IModelService modelService;
    private final MemoryProperties props;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Override
    public CompressionResult compress(List<MemoryMessage> messages, ShortTermPolicy policy) {
        int retain = Math.min(messages.size(), Math.max(4, policy.getMaxMessages() / 10));
        List<MemoryMessage> retained = new ArrayList<>();
        List<MemoryMessage> toCompress = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            if (i >= messages.size() - retain) retained.add(messages.get(i));
            else toCompress.add(messages.get(i));
        }

        // 构造结构化提示词与对话内容
        String promptText = buildCompressionPrompt(toCompress);
        String summaryText = callCompressionModel(promptText);

        // 生成摘要块
        MemoryChunk chunk = MemoryChunk.builder()
                .id(UUID.randomUUID().toString())
                .title("对话结构化摘要")
                .summary(summaryText)
                .tags(List.of("dialog", "compressed", "structured"))
                .importanceScore(null)
                .createdAt(Instant.now())
                .lastAccessAt(Instant.now())
                .knowledgeId(null)
                .agentId(null)
                .build();

        String ratio = String.format("%d->%d", messages.size(), retained.size());
        return CompressionResult.builder()
                .retainedMessages(retained)
                .newChunks(List.of(chunk))
                .compressionRatio(ratio)
                .build();
    }

    private String buildCompressionPrompt(List<MemoryMessage> toCompress) {
        StringBuilder sb = new StringBuilder();
        sb.append("请分析以下对话内容，提供结构化摘要：\n")
          .append("1. Primary Request and Intent: 用户的主要请求和意图是什么\n")
          .append("2. Key Technical Concepts: 涉及的关键技术概念和专业术语\n")
          .append("3. Files and Code Sections: 相关的文件路径、代码段和具体位置\n")
          .append("4. Errors and fixes: 出现的错误、问题及其解决方案\n")
          .append("5. Problem Solving: 问题解决的思路、方法和结果\n")
          .append("6. All user messages: 所有用户消息的完整记录和时间线\n")
          .append("7. Pending Tasks: 待完成的任务、后续计划和优先级\n")
          .append("8. Current Work: 当前进行的工作、状态和下一步行动\n\n");

        sb.append("[对话原文]\n");
        for (MemoryMessage mm : toCompress) {
            String role = mm.getPayload() == null ? "message" : mm.getPayload().getClass().getSimpleName();
            String ts = mm.getCreatedAt() == null ? "-" : TS_FMT.format(mm.getCreatedAt());
            String text = mm.getPayload() == null ? "" : MessageUtils.extractText(mm.getPayload());
            if (text.isEmpty()) continue;
            sb.append("- [").append(ts).append("] ").append(role).append(": ")
              .append(text).append('\n');
        }
        return sb.toString();
    }

    private String callCompressionModel(String promptText) {
        try {
            String compressionModelId = props.getCompression().getModelId();
            if (compressionModelId == null || compressionModelId.isBlank()) {
                log.warn("Compression model id is not configured. Falling back to raw text.");
                return promptText;
            }
            ChatModel chatModel = modelService.getLatestChatModel(compressionModelId);
            UserMessage userMessage = UserMessage.builder().text(promptText).build();
            ChatResponse resp = chatModel.call(new Prompt(userMessage));
            return resp.getResults().get(0).getOutput().getText();
        } catch (Exception e) {
            log.warn("Compression model call failed: {}", e.getMessage());
            return promptText;
        }
    }
}
