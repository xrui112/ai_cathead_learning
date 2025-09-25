
package cn.cathead.ai.domain.exec.service.chain.factory.context;

import org.springframework.ai.chat.client.ChatClient;
import java.util.Map;

public interface ChainContext {
	ChatClient getChatClient();
	Map<String, Object> getParams();
}
