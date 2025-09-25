

package cn.cathead.ai.domain.exec.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import java.util.Map;

@Getter
@AllArgsConstructor
public class DefaultChainContext implements ChainContext {
	private final ChatClient chatClient;
	private final Map<String, Object> params;
}
