package cn.cathead.ai.domain.model.service.DynamicForm;

import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import cn.cathead.ai.domain.model.model.entity.ValidationResult;

import java.util.Map;

/**
 * 动态表单服务接口
 */
public interface IDynamicForm {
    
    /**
     * 根据provider和type获取表单配置
     * @return 表单配置
     */
    FormConfiguration getFormConfiguration(String provider, String type);
    
    /**
     * 校验表单数据
     * @param formData 表单数据
     * @return 校验结果
     */
    ValidationResult validateFormData(String provider, String type, Map<String, Object> formData);
    
    /**
     * 提交表单并创建模型
     * @param formData 表单数据
     * @return 创建的模型ID
     */
    String submitForm(String provider, String type, Map<String, Object> formData);
}
