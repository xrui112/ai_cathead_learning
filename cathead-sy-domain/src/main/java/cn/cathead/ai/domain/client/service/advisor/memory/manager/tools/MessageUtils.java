package cn.cathead.ai.domain.client.service.advisor.memory.manager.tools;

import org.springframework.ai.chat.messages.Message;

import java.lang.reflect.Method;

public final class MessageUtils {

    private MessageUtils() {}

    public static String extractText(Message message) {
        if (message == null) return "";
        String text = tryInvoke(message, "getContent");
        if (text != null) return text;
        text = tryInvoke(message, "getText");
        if (text != null) return text;
        return message.toString();
    }

    private static String tryInvoke(Message message, String methodName) {
        try {
            Method m = message.getClass().getMethod(methodName);
            Object res = m.invoke(message);
            return res == null ? null : String.valueOf(res);
        } catch (Exception ignore) {
            return null;
        }
    }
}


