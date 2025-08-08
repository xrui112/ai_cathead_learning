package cn.cathead.ai.types.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequestDTO implements Serializable {

    private String modelId;

    private String prompt;

    private Boolean stream;

    private Boolean OnlyText;

} 