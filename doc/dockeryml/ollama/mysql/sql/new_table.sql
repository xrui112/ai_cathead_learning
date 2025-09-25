
CREATE database if NOT EXISTS `ai_learning` default character set utf8mb4 collate utf8mb4_0900_ai_ci;
use `ai_learning`;

-- 1. ChatClient核心表
DROP TABLE IF EXISTS `chat_client`;
CREATE TABLE `chat_client` (
    `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
    `client_id` VARCHAR(64) NOT NULL COMMENT '客户端ID（全局唯一）',
    `client_name` VARCHAR(128) NOT NULL COMMENT '客户端名称',
    `description` VARCHAR(1024) DEFAULT NULL COMMENT '描述',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE:启用,INACTIVE:禁用,DELETED:已删除)',
    `enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    
    -- 系统字段
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_client_id` (`client_id`),
    KEY `idx_status` (`status`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ChatClient配置表';

-- 2. 模型配置表（统一chat和embedding模型）
DROP TABLE IF EXISTS `model`;
CREATE TABLE `model` (
    `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
    `model_id` VARCHAR(64) NOT NULL COMMENT '模型ID（全局唯一）',
    `provider_name` VARCHAR(64) NOT NULL COMMENT '提供商名称(openai/deepseek/ollama等)',
    `model_name` VARCHAR(128) NOT NULL COMMENT '模型名称',
    `url` VARCHAR(512) NOT NULL COMMENT 'API服务URL',
    `key` VARCHAR(512) NOT NULL COMMENT 'API密钥',
    `type` VARCHAR(32) NOT NULL COMMENT '模型类型(chat/embedding)',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE:启用,INACTIVE:禁用,DELETED:已删除)',
    `enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    
    -- Chat模型参数
    `temperature` FLOAT DEFAULT NULL COMMENT '温度参数',
    `top_p` FLOAT DEFAULT NULL COMMENT 'Top P参数',
    `max_tokens` INT DEFAULT NULL COMMENT '最大token数',
    `stop` VARCHAR(1024) DEFAULT NULL COMMENT '停止词（JSON数组字符串）',
    `frequency_penalty` FLOAT DEFAULT NULL COMMENT '频率惩罚',
    `presence_penalty` FLOAT DEFAULT NULL COMMENT '存在惩罚',
    `system_prompt` TEXT DEFAULT NULL COMMENT '系统提示词',
    `max_context_length` INT DEFAULT NULL COMMENT '最大上下文长度',
    `support_stream` BOOLEAN DEFAULT TRUE COMMENT '是否支持流式输出',
    `support_function_call` BOOLEAN DEFAULT FALSE COMMENT '是否支持函数调用',
    
    -- Embedding模型参数
    `embedding_format` VARCHAR(32) DEFAULT NULL COMMENT '向量格式',
    `num_predict` INT DEFAULT NULL COMMENT '预测数量参数',
    `dimensions` INT DEFAULT NULL COMMENT '向量维度',
    `max_input_length` INT DEFAULT NULL COMMENT '最大输入长度',
    `support_batch` BOOLEAN DEFAULT FALSE COMMENT '是否支持批量处理',
    `max_batch_size` INT DEFAULT NULL COMMENT '批量处理最大大小',
    `normalize` BOOLEAN DEFAULT TRUE COMMENT '是否归一化向量',
    `similarity_metric` VARCHAR(32) DEFAULT 'cosine' COMMENT '相似度计算方式(cosine/euclidean/dot)',
    
    -- 动态属性字段（JSON格式存储扩展参数）
    `dynamic_properties` JSON DEFAULT NULL COMMENT '动态属性，存储模型的扩展参数',
    
    -- 系统字段
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
    
    -- 乐观锁版本字段
    `version` BIGINT DEFAULT 0 NOT NULL COMMENT '乐观锁版本号',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_id` (`model_id`),
    KEY `idx_provider_name` (`provider_name`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型配置表（统一chat和embedding）';

-- 3. 模型校验规则表
DROP TABLE IF EXISTS `model_validation_rule`;
CREATE TABLE `model_validation_rule` (
    `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
    `provider_name` VARCHAR(64) NOT NULL COMMENT '提供商名称',
    `model_type` VARCHAR(32) NOT NULL COMMENT '模型类型(chat/embedding)',
    `field_name` VARCHAR(64) NOT NULL COMMENT '字段名称',
    
    -- 校验规则配置
    `field_type` VARCHAR(32) NOT NULL COMMENT '字段类型(string/number/boolean/array)',
    `required` BOOLEAN DEFAULT FALSE COMMENT '是否必填',
    `default_value` VARCHAR(512) DEFAULT NULL COMMENT '默认值',
    
    -- 数值范围校验
    `min_value` DECIMAL(10,4) DEFAULT NULL COMMENT '最小值',
    `max_value` DECIMAL(10,4) DEFAULT NULL COMMENT '最大值',
    
    -- 字符串长度校验
    `min_length` INT DEFAULT NULL COMMENT '最小长度',
    `max_length` INT DEFAULT NULL COMMENT '最大长度',
    
    -- 正则表达式校验
    `pattern` VARCHAR(512) DEFAULT NULL COMMENT '正则表达式',
    
    -- 枚举值校验
    `enum_values` VARCHAR(1024) DEFAULT NULL COMMENT '枚举值列表（逗号分隔）',
    
    -- 自定义校验逻辑
    `custom_validator` VARCHAR(256) DEFAULT NULL COMMENT '自定义校验器类名',
    
    -- 错误信息
    `error_message` VARCHAR(512) DEFAULT NULL COMMENT '校验失败错误信息',
    
    -- 字段描述和提示
    `field_label` VARCHAR(128) DEFAULT NULL COMMENT '字段标签',
    `field_description` VARCHAR(512) DEFAULT NULL COMMENT '字段描述',
    `placeholder` VARCHAR(256) DEFAULT NULL COMMENT '占位符文本',
    
    -- 状态和时间字段
    `enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider_type_field` (`provider_name`, `model_type`, `field_name`),
    KEY `idx_provider_name` (`provider_name`),
    KEY `idx_model_type` (`model_type`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型校验规则表';

-- 4. RAG配置表
DROP TABLE IF EXISTS `rag_config`;
CREATE TABLE `rag_config` (
    `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
    `rag_id` VARCHAR(64) NOT NULL COMMENT 'RAG配置ID（全局唯一）',
    `client_id` VARCHAR(64) NOT NULL COMMENT '关联的客户端ID',
    `rag_name` VARCHAR(128) NOT NULL COMMENT 'RAG配置名称',
    
    -- RAG服务配置
    `endpoint` VARCHAR(512) NOT NULL COMMENT 'RAG服务端点',
    `api_key` VARCHAR(512) DEFAULT NULL COMMENT 'RAG服务API密钥',
    `index_name` VARCHAR(128) NOT NULL COMMENT '索引名称',
    `knowledge_tag` VARCHAR(128) DEFAULT NULL COMMENT '知识标签',
    
    -- 检索参数
    `top_k` INT DEFAULT 4 COMMENT 'TopK检索数量',
    `similarity_threshold` FLOAT DEFAULT 0.7 COMMENT '相似度阈值',
    `filter_expression` VARCHAR(512) DEFAULT NULL COMMENT '过滤表达式',
    
    -- 扩展配置
    `ext_config` JSON DEFAULT NULL COMMENT '扩展配置（JSON格式）',
    
    -- 状态和时间字段
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE:启用,INACTIVE:禁用,DELETED:已删除)',
    `enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',

    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rag_id` (`rag_id`),
    KEY `idx_client_id` (`client_id`),
    KEY `idx_knowledge_tag` (`knowledge_tag`),
    KEY `idx_status` (`status`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG配置表';

-- 5. MCP工具配置表
DROP TABLE IF EXISTS `mcp_tool_config`;
CREATE TABLE `mcp_tool_config` (
    `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
    `mcp_id` VARCHAR(64) NOT NULL COMMENT 'MCP工具ID（全局唯一）',
    `client_id` VARCHAR(64) NOT NULL COMMENT '关联的客户端ID',
    `mcp_name` VARCHAR(128) NOT NULL COMMENT 'MCP工具名称',
    
    -- 传输配置
    `transport_type` VARCHAR(20) NOT NULL COMMENT '传输类型(sse/stdio)',
    `transport_config` JSON NOT NULL COMMENT '传输配置（JSON格式）',
    `request_timeout` INT DEFAULT 180 COMMENT '请求超时时间(秒)',
    
    -- 状态和时间字段
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE:启用,INACTIVE:禁用,DELETED:已删除)',
    `enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',

    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mcp_id` (`mcp_id`),
    KEY `idx_client_id` (`client_id`),
    KEY `idx_transport_type` (`transport_type`),
    KEY `idx_status` (`status`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP工具配置表';

-- 6. Agent配置表（可选，用于任务调度）
DROP TABLE IF EXISTS `ai_agent`;
CREATE TABLE `ai_agent` (
    `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
    `agent_id` VARCHAR(64) NOT NULL COMMENT '智能体ID',
    `agent_name` VARCHAR(128) NOT NULL COMMENT '智能体名称',
    `description` VARCHAR(1024) DEFAULT NULL COMMENT '描述',
    `channel` VARCHAR(32) DEFAULT NULL COMMENT '渠道类型(agent，chat_stream)',
    
    -- 状态和时间字段
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE:启用,INACTIVE:禁用,DELETED:已删除)',
    `enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_id` (`agent_id`),
    KEY `idx_status` (`status`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI智能体配置表';

-- 7. Agent-Client关联表
DROP TABLE IF EXISTS `agent_client_relation`;
CREATE TABLE `agent_client_relation` (
    `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
    `agent_id` VARCHAR(64) NOT NULL COMMENT '智能体ID',
    `client_id` VARCHAR(64) NOT NULL COMMENT '客户端ID',
    `sequence` INT NOT NULL DEFAULT 1 COMMENT '执行顺序',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_client_seq` (`agent_id`, `client_id`, `sequence`),
    KEY `idx_agent_id` (`agent_id`),
    KEY `idx_client_id` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent-Client关联表';

-- 8. 任务调度配置表
DROP TABLE IF EXISTS `agent_task_schedule`;
CREATE TABLE `agent_task_schedule` (
    `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
    `agent_id` VARCHAR(64) NOT NULL COMMENT '智能体ID',
    `task_name` VARCHAR(128) DEFAULT NULL COMMENT '任务名称',
    `description` VARCHAR(1024) DEFAULT NULL COMMENT '任务描述',
    `cron_expression` VARCHAR(50) NOT NULL COMMENT 'Cron表达式',
    `task_param` JSON DEFAULT NULL COMMENT '任务参数（JSON格式）',
    
    -- 状态和时间字段
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE:启用,INACTIVE:禁用,DELETED:已删除)',
    `enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
    
    PRIMARY KEY (`id`),
    KEY `idx_agent_id` (`agent_id`),
    KEY `idx_status` (`status`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent任务调度配置表';


# -- =====================================================
# -- 初始化数据
# -- =====================================================
#
# -- 插入校验规则数据
# INSERT INTO `model_validation_rule` (`provider_name`, `model_type`, `field_name`, `field_type`, `required`, `default_value`, `min_value`, `max_value`, `error_message`, `field_label`, `field_description`, `placeholder`) VALUES
# -- OpenAI Chat模型校验规则
# ('openai', 'chat', 'modelName', 'string', TRUE, NULL, NULL, NULL, '模型名称不能为空', '模型名称', 'OpenAI模型名称，如gpt-3.5-turbo', '请输入模型名称，如：gpt-3.5-turbo'),
# ('openai', 'chat', 'url', 'string', TRUE, 'https://api.openai.com', NULL, NULL, 'API基础URL不能为空', 'API基础URL', 'OpenAI API的基础URL地址', '请输入API基础URL'),
# ('openai', 'chat', 'key', 'string', TRUE, NULL, NULL, NULL, 'API密钥不能为空', 'API密钥', 'OpenAI API密钥', '请输入API密钥'),
# ('openai', 'chat', 'temperature', 'number', FALSE, '0.7', 0, 2, '温度参数必须在0-2之间', '温度参数', '控制输出的随机性，0表示确定性输出，2表示最大随机性', '0.7'),
# ('openai', 'chat', 'topP', 'number', FALSE, '1.0', 0, 1, 'TopP参数必须在0-1之间', 'TopP参数', '核采样参数，控制输出的多样性', '1.0'),
# ('openai', 'chat', 'maxTokens', 'number', FALSE, '2048', 1, 32768, '最大令牌数必须大于0且不超过32768', '最大令牌数', '单次对话的最大令牌数限制', '2048'),
# ('openai', 'chat', 'frequencyPenalty', 'number', FALSE, '0.0', -2, 2, '频率惩罚必须在-2到2之间', '频率惩罚', '降低重复内容的概率', '0.0'),
# ('openai', 'chat', 'presencePenalty', 'number', FALSE, '0.0', -2, 2, '存在惩罚必须在-2到2之间', '存在惩罚', '鼓励谈论新话题', '0.0'),
#
# -- OpenAI Embedding模型校验规则
# ('openai', 'embedding', 'modelName', 'string', TRUE, NULL, NULL, NULL, '模型名称不能为空', '模型名称', 'OpenAI嵌入模型名称，如text-embedding-ada-002', '请输入嵌入模型名称'),
# ('openai', 'embedding', 'url', 'string', TRUE, 'https://api.openai.com', NULL, NULL, 'API基础URL不能为空', 'API基础URL', 'OpenAI API的基础URL地址', '请输入API基础URL'),
# ('openai', 'embedding', 'key', 'string', TRUE, NULL, NULL, NULL, 'API密钥不能为空', 'API密钥', 'OpenAI API密钥', '请输入API密钥'),
# ('openai', 'embedding', 'dimensions', 'number', FALSE, '1536', 1, 3072, '向量维度必须大于0且不超过3072', '向量维度', '嵌入向量的维度', '1536'),
#
# -- DeepSeek Chat模型校验规则
# ('deepseek', 'chat', 'modelName', 'string', TRUE, NULL, NULL, NULL, '模型名称不能为空', '模型名称', 'DeepSeek模型名称', '请输入模型名称'),
# ('deepseek', 'chat', 'url', 'string', TRUE, 'https://api.deepseek.com', NULL, NULL, 'API基础URL不能为空', 'API基础URL', 'DeepSeek API的基础URL地址', '请输入API基础URL'),
# ('deepseek', 'chat', 'key', 'string', TRUE, NULL, NULL, NULL, 'API密钥不能为空', 'API密钥', 'DeepSeek API密钥', '请输入API密钥'),
# ('deepseek', 'chat', 'temperature', 'number', FALSE, '0.7', 0, 2, '温度参数必须在0-2之间', '温度参数', '控制输出的随机性', '0.7'),
#
# -- Ollama Chat模型校验规则
# ('ollama', 'chat', 'modelName', 'string', TRUE, NULL, NULL, NULL, '模型名称不能为空', '模型名称', 'Ollama模型名称', '请输入模型名称'),
# ('ollama', 'chat', 'url', 'string', TRUE, 'http://localhost:11434', NULL, NULL, 'API基础URL不能为空', 'API基础URL', 'Ollama API的基础URL地址', '请输入API基础URL'),
# ('ollama', 'chat', 'temperature', 'number', FALSE, '0.8', 0, 1, '温度参数必须在0-1之间', '温度参数', '控制输出的随机性', '0.8'),
#
# -- Ollama Embedding模型校验规则
# ('ollama', 'embedding', 'modelName', 'string', TRUE, NULL, NULL, NULL, '模型名称不能为空', '模型名称', 'Ollama嵌入模型名称', '请输入嵌入模型名称'),
# ('ollama', 'embedding', 'url', 'string', TRUE, 'http://localhost:11434', NULL, NULL, 'API基础URL不能为空', 'API基础URL', 'Ollama API的基础URL地址', '请输入API基础URL');
#
# -- 示例数据
# INSERT INTO `chat_client` (`client_id`, `client_name`, `description`, `creator`) VALUES
# ('client_001', '智能对话助手', '基于GPT的智能对话助手', 'system'),
# ('client_002', '文档分析助手', '专门用于文档分析和总结的助手', 'system');
#
# INSERT INTO `model_config` (`model_id`, `client_id`, `model_name`, `provider_name`, `url`, `key`, `type`, `temperature`, `max_tokens`, `creator`) VALUES
# ('model_001', 'client_001', 'gpt-3.5-turbo', 'openai', 'https://api.openai.com', 'sk-your-api-key', 'chat', 0.7, 2048, 'system'),
# ('model_002', 'client_002', 'gpt-4', 'openai', 'https://api.openai.com', 'sk-your-api-key', 'chat', 0.5, 4096, 'system'),
# ('model_003', 'client_001', 'text-embedding-ada-002', 'openai', 'https://api.openai.com', 'sk-your-api-key', 'embedding', NULL, NULL, 'system');
#
# -- =====================================================
# -- 索引优化建议
# -- =====================================================
#
# -- 为经常查询的字段添加复合索引
# CREATE INDEX `idx_model_client_type` ON `model_config` (`client_id`, `type`, `enabled`);
# CREATE INDEX `idx_model_provider_type` ON `model_config` (`provider_name`, `type`, `enabled`);
# CREATE INDEX `idx_validation_provider_type_enabled` ON `model_validation_rule` (`provider_name`, `model_type`, `enabled`);
#
# -- =====================================================
# -- 视图定义（可选，便于查询）
# -- =====================================================
#
# -- 活跃的Chat模型视图
# CREATE OR REPLACE VIEW `v_active_chat_models` AS
# SELECT
#     mc.model_id,
#     mc.client_id,
#     mc.provider_name,
#     mc.model_name,
#     mc.url,
#     mc.temperature,
#     mc.max_tokens,
#     cc.client_name,
#     cc.description as client_description
# FROM `model_config` mc
# JOIN `chat_client` cc ON mc.client_id = cc.client_id
# WHERE mc.type = 'chat'
#   AND mc.enabled = TRUE
#   AND mc.status = 'ACTIVE'
#   AND cc.enabled = TRUE
#   AND cc.status = 'ACTIVE';
#
# -- 活跃的Embedding模型视图
# CREATE OR REPLACE VIEW `v_active_embedding_models` AS
# SELECT
#     mc.model_id,
#     mc.client_id,
#     mc.provider_name,
#     mc.model_name,
#     mc.url,
#     mc.dimensions,
#     mc.max_input_length,
#     cc.client_name,
#     cc.description as client_description
# FROM `model` mc
# JOIN `chat_client` cc ON mc.client_id = cc.client_id
# WHERE mc.type = 'embedding'
#   AND mc.enabled = TRUE
#   AND mc.status = 'ACTIVE'
#   AND cc.enabled = TRUE
#   AND cc.status = 'ACTIVE';
