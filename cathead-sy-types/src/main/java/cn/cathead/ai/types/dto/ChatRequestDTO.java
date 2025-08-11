package cn.cathead.ai.types.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequestDTO implements Serializable {

    private String modelId;

    private String prompt;

    private Boolean stream;

    private Boolean onlyText;

    private byte[] image;

    private String imageDescription;

} 
