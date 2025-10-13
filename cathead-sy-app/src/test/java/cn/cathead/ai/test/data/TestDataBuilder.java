package cn.cathead.ai.test.data;

import cn.cathead.ai.types.dto.ChatModelDTO;
import cn.cathead.ai.types.dto.EmbeddingModelDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TestDataBuilder {

    /**
     * Ollama:ChatModelDTO
     */
    public static ChatModelDTO.ChatModelDTOBuilder defaultChatModelDTO() {
        return ChatModelDTO.builder()
                .providerName("ollama")
                .modelName("qwen3")
                .url("http://localhost:11434")
                .key("")
                .type("chat")
                .temperature(0.7f)
                .topP(0.9f)
                .maxTokens(2048)
                .frequencyPenalty(0.0f)
                .presencePenalty(0.0f)
                .stop(new String[]{"[INST]", "[/INST]"});
    }

    /**
     * OpenAI:ChatModelDTO
     */
    public static ChatModelDTO.ChatModelDTOBuilder openaiChatModelDTO() {
        return ChatModelDTO.builder()
                .providerName("openai")
                .modelName("MODELNAME")
                .url("OPENAIURL")
                .key("OPENAIKEY!!!!!")
                .type("chat")
                .temperature(0.8f)
                .topP(1.0f)
                .maxTokens(4096)
                .frequencyPenalty(0.1f)
                .presencePenalty(0.1f)
                .stop(new String[]{"Human:", "Assistant:"});
    }

    /**
     * Ollama:EmbeddingModelDTO nomic-embed-text
     */
    public static EmbeddingModelDTO.EmbeddingModelDTOBuilder defaultEmbeddingModelDTO() {
        return EmbeddingModelDTO.builder()
                .providerName("ollama")
                .modelName("nomic-embed-text")
                .url("http://localhost:11434")
                .key("")
                .type("embedding")
                .embeddingFormat("json")
                .numPredict(512);
    }

    /**
     * OpenAI:EmbeddingModelDTO
     */
    public static EmbeddingModelDTO.EmbeddingModelDTOBuilder openaiEmbeddingModelDTO() {
        return EmbeddingModelDTO.builder()
                .providerName("openai")
                .modelName("text-embedding-ada-002")
                .url("h")
                .key("")
                .type("embedding")
                .embeddingFormat("float")
                .numPredict(1536);
    }

    /**
     * 构建无效的ChatModelDTO（用于参数校验测试）
     */
    public static ChatModelDTO.ChatModelDTOBuilder invalidChatModelDTO() {
        return ChatModelDTO.builder()
                .providerName("") // 空的提供商
                .modelName("") // 空的模型名
                .url("invalid-url") // 无效的URL
                .type("chat")
                .temperature(3.0f) // 超出范围的温度
                .topP(-1.0f) // 无效的topP
                .maxTokens(-100); // 无效的maxTokens
    }

    /**
     * todo 构建动态表单测试数据
     */
    public static Map<String, Object> defaultFormData() {
        Map<String, Object> formData = new HashMap<>();
        //nomic-embed-text
        formData.put("modelName", "qwen-plus");
//        formData.put("temperature", 0.7);
        formData.put("url", "https://dashscope.aliyuncs.com/compatible-mode");
        formData.put("key","sk-c39abd1f69684b8db0046d82daf0676a");
//        formData.put("dimensions",1024);
//        formData.put("topP", 0.9);
//        formData.put("maxTokens", 4096);
        return formData;
    }

    /**
     * 构建无效的动态表单数据
     */
    public static Map<String, Object> invalidFormData() {
        Map<String, Object> formData = new HashMap<>();
        formData.put("modelName", ""); // 空名称
        formData.put("temperature", 3.0); // 超出范围
        formData.put("url", "invalid-url"); // 无效URL
        return formData;
    }

    /**
     * 构建更新用的ChatModelDTO（温度略有不同）
     */
    public static ChatModelDTO.ChatModelDTOBuilder updateChatModelDTO() {
        return defaultChatModelDTO()
                .temperature(0.8f) // 修改温度
                .maxTokens(3000); // 修改最大token数
    }

    /**
     * 构建更新用的EmbeddingModelDTO
     */
    public static EmbeddingModelDTO.EmbeddingModelDTOBuilder updateEmbeddingModelDTO() {
        return defaultEmbeddingModelDTO()
                .numPredict(1024) // 修改预测数量
                .embeddingFormat("float"); // 修改格式
    }

    /**
     * 生成随机的模型ID用于测试
     */
    public static String randomModelId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成不存在的模型ID用于测试
     */
    public static String nonExistentModelId() {
        return "non-existent-" + UUID.randomUUID().toString();
    }
} 