package cn.cathead.ai.domain.model.service.provider.providerimpl;

import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.model.valobj.ModelPropertyVo;
import cn.cathead.ai.domain.model.service.provider.IModelProvider;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component("ollama")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OllamaProvider implements IModelProvider {

    //初次创建
    @Override
    public OllamaChatModel createChat(ChatModelEntity chatModelEntity) {
        OllamaApi ollamaApi = new OllamaApi.Builder()
                .baseUrl(chatModelEntity.getUrl())
                .build();

        return new OllamaChatModel(
                ollamaApi,
                OllamaOptions.builder()
                        .model(chatModelEntity.getModelName())
                        .temperature(Double.valueOf(chatModelEntity.getTemperature() == null
                                ? Float.valueOf(ModelPropertyVo.TEMPERATURE.getDefaultValue())
                                : chatModelEntity.getTemperature()))
                        .topP(Double.valueOf(chatModelEntity.getTopP() == null
                                ? Float.valueOf(ModelPropertyVo.TOP_K.getDefaultValue())
                                : chatModelEntity.getTopP()))
                        .numPredict(chatModelEntity.getMaxTokens() == null
                                ? Integer.valueOf(ModelPropertyVo.MAX_TOKENS.getDefaultValue())
                                : chatModelEntity.getMaxTokens())
                        .stop(List.of(chatModelEntity.getStop() == null || chatModelEntity.getStop().length == 0
                                ? ModelPropertyVo.STOP.getDefaultArray()
                                : chatModelEntity.getStop()))
                        .frequencyPenalty(Double.valueOf(chatModelEntity.getFrequencyPenalty() == null
                                ? Float.valueOf(ModelPropertyVo.FREQUENCY_PENALTY.getDefaultValue())
                                : chatModelEntity.getFrequencyPenalty()))
                        .presencePenalty(Double.valueOf(chatModelEntity.getPresencePenalty() == null
                                ? Float.valueOf(ModelPropertyVo.PRESENCE_PENALTY.getDefaultValue())
                                : chatModelEntity.getPresencePenalty()))
                        .build(),
                ToolCallingManager
                        .builder()
                        .build(),
                ObservationRegistry.NOOP,
                ModelManagementOptions
                        .builder()
                        .build()
        );
    }

    @Override
    public OllamaEmbeddingModel createEmbedding(EmbeddingModelEntity embeddingModelEntity) {
        OllamaApi ollamaApi = new OllamaApi.Builder()
                .baseUrl(embeddingModelEntity.getUrl())
                .build(); // Ollama 也不需要 key

        return new OllamaEmbeddingModel(
                ollamaApi,
                OllamaOptions.builder()
                        .model(embeddingModelEntity.getModelName())
                        .format(embeddingModelEntity.getEmbeddingFormat() == null
                                ? ModelPropertyVo.EMBEDDIDNGFORMAT.getDefaultValue()
                                : embeddingModelEntity.getEmbeddingFormat())
                        .numPredict(embeddingModelEntity.getNumPredict() == null
                                ? Integer.valueOf(ModelPropertyVo.NUMPREDICT.getDefaultValue())
                                : embeddingModelEntity.getNumPredict())
                        .build(),
                ObservationRegistry.NOOP,
                ModelManagementOptions.builder().build()
        );
    }


}
