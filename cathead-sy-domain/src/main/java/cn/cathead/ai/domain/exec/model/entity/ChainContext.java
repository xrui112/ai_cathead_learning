
package cn.cathead.ai.domain.exec.model.entity;

import org.springframework.ai.chat.client.ChatClient;
import java.util.Map;

public interface ChainContext {
	ChatClient getChatClient();
	Map<String, Object> getParams();
}
