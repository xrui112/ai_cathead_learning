package cn.cathead.ai.domain.exec.service.chain.tools;

import cn.cathead.ai.domain.exec.model.entity.AutoAgentExecuteResultEntity;
import cn.cathead.ai.domain.exec.model.entity.LoopContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class SseUtils {
	private static final ObjectMapper MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	private SseUtils() {}

	@SneakyThrows
	public static void sendSseResult(LoopContext ctx, AutoAgentExecuteResultEntity result) {
		ResponseBodyEmitter emitter = ctx.getEmitter();
		if (emitter == null) return;
		AutoAgentExecuteResultEntity payload = result.getTimestamp() == null
				? AutoAgentExecuteResultEntity.builder()
				.stage(result.getStage())
				.subType(result.getSubType())
				.content(result.getContent())
				.meta(result.getMeta())
				.timestamp(Instant.now())
				.build()
				: result;
		String json = MAPPER.writeValueAsString(payload);
		emitter.send("data: " + json + "\n\n");
	}

	public static void sendSection(LoopContext ctx, String stage, String subType, String content) {
		Map<String, Object> meta = new HashMap<>();
		sendSseResult(ctx, AutoAgentExecuteResultEntity.builder()
				.stage(stage)
				.subType(subType)
				.content(content)
				.meta(meta)
				.build());
	}
}


