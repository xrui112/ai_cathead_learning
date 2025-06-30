package cn.cathead.ai.domain.model.model.valobj;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public enum ModelPropertyVo {

    /**
     *  temperature
     *
     *  top_p
     *
     *  max_tokens
     *
     *  stop
     *
     *  frequency_penalty
     *
     *  presence_penalty
     *
     *
     *  向量模型:
     *  embeddingFormat
     *
     *  stream
     *
     *  numPredict
     */

    //todo 设定默认值
    TEMPERATURE("0001", "0.7", "chat模型: 控制生成的随机性，范围 [0, 2]，越大越发散"),
    TOP_K("0002", "1.0", "chat模型: 核采样阈值（top_p），与 temperature 联合控制采样行为"),
    MAX_TOKENS("0003", "1024", "chat模型: 模型输出响应的最大 token 数，过大会消耗更多计算资源"),
    //                     "\\n,###"
    STOP("0004", "", "chat模型: 设置模型生成的停止词，多个时用数组；为空表示默认行为"),
    FREQUENCY_PENALTY("0005", "0.0", "chat模型: 对高频词的惩罚，范围 [0, 2]，提高多样性"),
    PRESENCE_PENALTY("0006", "0.0", "chat模型: 对已有词的惩罚，鼓励引入新词，范围 [0, 2]"),
    EMBEDDIDNGFORMAT("0007", "float", "embedding模型: 输出向量的格式，如 float、base64 等"),
    STREAM("0008", "true", "是否开启流式响应，true 表示使用分片增量响应"),
    NUMPREDICT("0009", "256", "embedding/llm模型: 预测生成 token 数量上限，仅部分模型支持")
    ;

    private final String code;
    private final String defaultValue;
    private final String info;

    public String[] getDefaultArray() {
        if (this == STOP && defaultValue != null && !defaultValue.isEmpty()) {
            return defaultValue.split("\\s*,\\s*"); // 支持逗号分隔
        }
        return new String[0];
    }

}
