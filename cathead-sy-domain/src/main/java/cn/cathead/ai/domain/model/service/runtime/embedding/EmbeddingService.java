package cn.cathead.ai.domain.model.service.runtime.embedding;

import cn.cathead.ai.domain.model.service.registry.IModelProviderService;
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
public class EmbeddingService implements IEmbeddingService {

    private final IModelProviderService modelProviderService;

    public EmbeddingService(IModelProviderService modelProviderService) {
        this.modelProviderService = modelProviderService;
    }

    @Override
    public EmbeddingResponse embedText(EmbeddingRequestDTO embeddingRequestDto) {
        log.info("调用Embedding接口，模型ID: {}", embeddingRequestDto.getModelId());
        EmbeddingModel embeddingModel = modelProviderService.getAndValidateEmbeddingModel(embeddingRequestDto.getModelId());
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
            String errorMsg = String.format("文本向量化处理失败: %s", e.getMessage());
            log.error(errorMsg, e);
            throw new AppException(ResponseCode.FAILED_EMBEDDING.getCode(), errorMsg);
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

}