package cn.cathead.ai.domain.model.service.provider.providerImpl;

import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.model.valobj.ModelPropertyVo;
import cn.cathead.ai.domain.model.service.provider.ModelProvider;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component("openaiprovider")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OpenaiProvider implements ModelProvider {
    @Override
    public ChatModel createChat(ChatModelEntity chatModelEntity) {
         OpenAiApi openAiApi= OpenAiApi
                 .builder()
                .baseUrl(chatModelEntity.getUrl())
                .apiKey(chatModelEntity.getKey())
                .build();

        OpenAiChatOptions options = OpenAiChatOptions
                .builder()
                .model(chatModelEntity.getModelName())
                .temperature(chatModelEntity.getTemperature() == null
                        ? Double.parseDouble(ModelPropertyVo.TEMPERATURE.getDefaultValue())
                        : chatModelEntity.getTemperature())
                .topP(chatModelEntity.getTopP() == null
                        ? Double.parseDouble(ModelPropertyVo.TOP_K.getDefaultValue())
                        : chatModelEntity.getTopP())
                .maxTokens(chatModelEntity.getMaxTokens() == null
                        ? Integer.parseInt(ModelPropertyVo.MAX_TOKENS.getDefaultValue())
                        : chatModelEntity.getMaxTokens())
                .stop(chatModelEntity.getStop() == null || chatModelEntity.getStop().length == 0
                        ? List.of(ModelPropertyVo.STOP.getDefaultArray())
                        : List.of(chatModelEntity.getStop()))
                .frequencyPenalty(chatModelEntity.getFrequencyPenalty() == null
                        ? Double.parseDouble(ModelPropertyVo.FREQUENCY_PENALTY.getDefaultValue())
                        : chatModelEntity.getFrequencyPenalty())
                .presencePenalty(chatModelEntity.getPresencePenalty() == null
                        ? Double.parseDouble(ModelPropertyVo.PRESENCE_PENALTY.getDefaultValue())
                        : chatModelEntity.getPresencePenalty())
                .build();

        return new OpenAiChatModel(
                openAiApi,
                options,
                ToolCallingManager.builder().build(),
                RetryTemplate.defaultInstance(),
                ObservationRegistry.NOOP
        );
    }

    @Override
    public EmbeddingModel createEmbedding(EmbeddingModelEntity embeddingModelEntity) {
        OpenAiApi client = OpenAiApi.builder()
                .baseUrl(embeddingModelEntity.getUrl())
                .apiKey(embeddingModelEntity.getKey())
                .build();


        MetadataMode metadataMode=MetadataMode.ALL;
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(embeddingModelEntity.getModelName())
                .build(); // OpenAI 的嵌入模型一般只需 model
        return new OpenAiEmbeddingModel(client, metadataMode,options);
    }
}
