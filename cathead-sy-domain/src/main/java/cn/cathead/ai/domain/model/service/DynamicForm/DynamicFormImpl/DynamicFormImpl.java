//package cn.cathead.ai.domain.model.service.DynamicForm.DynamicFormImpl;
//
//import cn.cathead.ai.domain.model.service.DynamicForm.IDynamicForm;
//import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
//import cn.cathead.ai.domain.model.model.entity.ValidationResult;
//import cn.cathead.ai.domain.model.model.entity.FieldDefinition;
//import cn.cathead.ai.domain.model.model.entity.FieldValidation;
//import cn.cathead.ai.domain.model.model.valobj.FieldType;
//import cn.cathead.ai.domain.model.service.DynamicForm.FormConfigurationManager.FormConfigurationManager;
//import cn.cathead.ai.domain.model.service.IModelService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//
//import java.util.Map;
//import java.util.regex.Pattern;
//
///**
// * 动态表单实现类
// */
//@Component
//@Slf4j
//public class DynamicFormImpl implements IDynamicForm {
//
//    @Autowired
//    private FormConfigurationManager formConfigurationManager;
//
//    @Autowired
//    private IModelService modelService;
//
//    /**
//     * 根据provider和type获取表单配置
//     */
//    @Override
//    public FormConfiguration getFormConfiguration(String provider, String type) {
//        log.info("获取表单配置，provider: {}, type: {}", provider, type);
//        // 委托给配置管理器
//        return formConfigurationManager.getFormConfiguration(provider, type);
//    }
//
//    /**
//     * 校验表单数据
//     */
//    @Override
//    public ValidationResult validateFormData(String provider, String type, Map<String, Object> formData) {
//        log.info("校验表单数据，provider: {}, type: {}, data: {}", provider, type, formData);
//
//        ValidationResult result = new ValidationResult();
//
//        // 获取表单配置
//        FormConfiguration config = getFormConfiguration(provider, type);
//        if (config == null) {
//            result.addError("system", "不支持的提供商或类型");
//            return result;
//        }
//
//        // todo 校验每个字段
//
//
//        return result;
//    }
//
//    /**
//     * 提交表单并创建模型
//     */
//    @Override
//    public String submitForm(String provider, String type, Map<String, Object> formData) {
//        log.info("提交表单，provider: {}, type: {}, data: {}", provider, type, formData);
//
//        // 先校验数据
//        ValidationResult validationResult = validateFormData(provider, type, formData);
//
//        if (!validationResult.isValid()) {
//            throw new RuntimeException("表单数据校验失败: " + validationResult.getAllErrors());
//        }
//
//        // 构建模型配置 chatModelEntity or EmbeddingEntity
//
//
//        // 调用模型服务创建模型
//        return modelService;
//    }
//
//
//    /**
//     * 构建模型配置 👌
//     */
//    private chatModelEntity/Embdding buildModelConfig(String provider, String type, Map<String, Object> formData) {
//
//    }
//
//}
