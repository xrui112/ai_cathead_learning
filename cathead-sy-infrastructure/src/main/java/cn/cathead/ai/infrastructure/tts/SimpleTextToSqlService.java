package cn.cathead.ai.infrastructure.tts;

import cn.cathead.ai.domain.exec.model.entity.ChainContext;
import cn.cathead.ai.domain.exec.service.tts.TextToSqlService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 简版 Text-to-SQL：当前用 LLM 生成只读 SQL，并附加基本安全约束（只许 SELECT + LIMIT）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimpleTextToSqlService implements TextToSqlService {

    @Override
    public String generateSql(String intent, JsonNode schemaOrSource, JsonNode constraints, ChainContext chainContext) {
        String schemaHint = schemaOrSource == null ? "" : schemaOrSource.toString();
        String constraintHint = constraints == null ? "" : constraints.toString();
        String prompt = "根据以下意图与约束生成 SQL（仅限 SELECT，不得包含 INSERT/UPDATE/DELETE/DROP；必须包含 LIMIT 1000）\n" +
                "[INTENT]\n" + intent + "\n[SCHEMA]\n" + schemaHint + "\n[CONSTRAINTS]\n" + constraintHint + "\n" +
                "仅输出 SQL，不要解释。";
        ChatClient client = chainContext.getChatClient();
        String sql = client.prompt(prompt).call().content();
        if (sql == null) sql = "";
        sql = sql.trim();
        // 基础安全处理：只允许 select，强制 limit
        String upper = sql.toUpperCase();
        if (!upper.startsWith("SELECT")) {
            throw new IllegalArgumentException("只允许 SELECT 查询");
        }
        if (!upper.contains(" LIMIT ")) {
            sql = sql + " LIMIT 1000";
        }
        return sql;
    }
}


