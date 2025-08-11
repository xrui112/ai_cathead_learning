package cn.cathead.ai.domain.model.service.embedding;

import cn.cathead.ai.domain.model.service.base.AbstractModelService;
import cn.cathead.ai.domain.model.service.modelcache.IModelCacheManager;
import cn.cathead.ai.types.dto.EmbeddingRequestDTO;
import cn.cathead.ai.types.enums.ResponseCode;
import cn.cathead.ai.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
public class EmbeddingService extends AbstractModelService<EmbeddingModel> implements IEmbeddingService {

    public EmbeddingService(IModelCacheManager modelCacheManager) {
        super(modelCacheManager, "Embedding");
    }

    @Override
    public EmbeddingResponse embedText(EmbeddingRequestDTO embeddingRequestDto) {
        EmbeddingModel embeddingModel = getAndValidateModel(embeddingRequestDto.getModelId());
        return generateEmbedding(embeddingModel, embeddingRequestDto);
    }

    @Override
    public EmbeddingResponse generateEmbedding(EmbeddingModel embeddingModel, EmbeddingRequestDTO request) {
        try {
            log.info("调用文本向量化处理");
            List<String> textsToEmbed = buildTextsList(request);

            if (textsToEmbed.isEmpty()) {
                throw new AppException(ResponseCode.FAILED_EMBEDDING.getCode(), "没有提供要向量化的文本");
            }

            EmbeddingRequest embeddingRequest = new EmbeddingRequest(textsToEmbed, null);
            return embeddingModel.call(embeddingRequest);
        } catch (Exception e) {
            throw handleModelCallException("文本向量化", e);
        }
    }

    private List<String> buildTextsList(EmbeddingRequestDTO request) {
        List<String> textsToEmbed = new ArrayList<>();

        if (request.getText() != null && !request.getText().trim().isEmpty()) {
            textsToEmbed.add(request.getText());
        }

        if (request.getTexts() != null && !request.getTexts().isEmpty()) {
            textsToEmbed.addAll(request.getTexts());
        }

        return textsToEmbed;
    }

    // 实现抽象方法
    @Override
    protected EmbeddingModel getModelFromCache(String modelId) {
        return modelCacheManager.ensureLatestEmbeddingModel(modelId);
    }

    @Override
    protected AppException createModelCallException(String message) {
        return new AppException(ResponseCode.FAILED_EMBEDDING.getCode(), message);
    }
}