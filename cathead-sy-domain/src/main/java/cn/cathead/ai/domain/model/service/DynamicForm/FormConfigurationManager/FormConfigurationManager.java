package cn.cathead.ai.domain.model.service.DynamicForm.FormConfigurationManager;

import cn.cathead.ai.domain.model.model.entity.FieldDefinition;
import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class FormConfigurationManager {
    
    private Map<String, FormConfiguration> configCache = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void loadConfigurations() {
        // 从YAML文件加载配置
    }
    
    public FormConfiguration getFormConfiguration(String provider, String type) {
        String key = provider + ":" + type;
        return configCache.get(key);
    }
    
    public List<FieldDefinition> getVisibleFields(String provider, String type) {
        FormConfiguration config = getFormConfiguration(provider, type);
        return config.getFields().stream()
                .filter(FieldDefinition::isVisible)
                .collect(Collectors.toList());
    }
}