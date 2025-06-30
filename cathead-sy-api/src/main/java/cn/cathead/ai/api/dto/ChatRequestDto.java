package cn.cathead.ai.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatRequestDto implements Serializable {


    private String modelId;

    private String prompt;

}
