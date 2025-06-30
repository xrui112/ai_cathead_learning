package cn.cathead.ai.infrastructure.persistent.po;

import lombok.Data;

@Data
public class ChatRequest {

    private String modelId;


    private String prompt;

}
