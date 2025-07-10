package cn.cathead.ai.domain.model.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型包装类，用于在缓存中存储模型实例和版本信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelWrapper<T> {
    /**
     * 模型实例
     */
    private T modelInstance;
    
    /**
     * 版本信息
     */
    private Long version;
    
    /**
     * 模型ID
     */
    private String modelId;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 最后访问时间
     */
    private Long lastAccessTime;
    
    /**
     * 更新最后访问时间
     */
    public void updateLastAccessTime() {
        this.lastAccessTime = System.currentTimeMillis();
    }
}