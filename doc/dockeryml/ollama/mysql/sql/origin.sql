-- ============================================
-- 完整的model表重建脚本（使用自增主键）
-- ============================================

-- 1. 删除原有表（如果存在）
DROP TABLE IF EXISTS model;

-- 2. 创建新的model表（自增主键 + version字段）
CREATE TABLE model (
                       id BIGINT AUTO_INCREMENT COMMENT '自增主键',
                       model_id VARCHAR(64) NOT NULL COMMENT '模型ID',
                       model_name VARCHAR(128) NOT NULL COMMENT '模型名称',
                       provider_name VARCHAR(64) NOT NULL COMMENT '提供商名称',
                       url VARCHAR(512) DEFAULT NULL COMMENT '模型URL',
                       `key` VARCHAR(256) DEFAULT NULL COMMENT '模型密钥',
                       type VARCHAR(32) NOT NULL COMMENT '模型类型: chat/embedding',

    -- Chat模型特有字段
                       temperature FLOAT DEFAULT NULL COMMENT '温度参数',
                       top_p FLOAT DEFAULT NULL COMMENT 'Top P参数',
                       max_tokens INT DEFAULT NULL COMMENT '最大token数',
                       stop VARCHAR(256) DEFAULT NULL COMMENT '停止词',
                       frequency_penalty FLOAT DEFAULT NULL COMMENT '频率惩罚',
                       presence_penalty FLOAT DEFAULT NULL COMMENT '存在惩罚',

    -- Embedding模型特有字段
                       embedding_format VARCHAR(64) DEFAULT NULL COMMENT 'Embedding格式',
                       num_predict INT DEFAULT NULL COMMENT '预测数量',

    -- 动态属性字段（JSON格式存储不常见的模型参数）
                       dynamic_properties JSON DEFAULT NULL COMMENT '动态属性，存储模型的扩展参数',

    -- 系统字段
                       create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                       update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 乐观锁版本字段
                       version BIGINT DEFAULT 0 NOT NULL COMMENT '乐观锁版本号',

    -- 主键和唯一约束
                       PRIMARY KEY (id),
                       UNIQUE KEY uk_model_id (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型配置表';

-- 3. 创建索引
CREATE INDEX idx_model_provider_type ON model(provider_name, type);
CREATE INDEX idx_model_id_version ON model(model_id, version);
CREATE INDEX idx_model_create_time ON model(create_time);
CREATE INDEX idx_model_update_time ON model(update_time);

-- 4. 插入测试数据（可选）
INSERT INTO model (
    model_id, model_name, provider_name, url, `key`, type,
    temperature, top_p, max_tokens, stop, frequency_penalty, presence_penalty,
    embedding_format, num_predict, dynamic_properties, version
) VALUES
      (
          'test-chat-001',
          'qwen2.5:7b',
          'ollama',
          'http://localhost:11434',
          NULL,
          'chat',
          0.7, 0.9, 2048, NULL, 0.0, 0.0,
          NULL, NULL, 
          JSON_OBJECT('seed', 42, 'top_k', 40, 'repeat_penalty', 1.1), 
          0
      ),
      (
          'test-embedding-001',
          'nomic-embed-text',
          'ollama',
          'http://localhost:11434',
          NULL,
          'embedding',
          NULL, NULL, NULL, NULL, NULL, NULL,
          'float', 512,
          JSON_OBJECT('normalize', true, 'truncate', true),
          0
      ),
      (
          'test-openai-001',
          'gpt-3.5-turbo',
          'openai',
          'https://api.openai.com/v1',
          'sk-your-api-key',
          'chat',
          0.8, 0.95, 4096, NULL, 0.1, 0.1,
          NULL, NULL,
          JSON_OBJECT('logit_bias', JSON_OBJECT('50256', -100), 'user', 'test-user'),
          0
      );

-- 5. 查看表结构
DESC model;

-- 6. 查看插入的测试数据
SELECT id, model_id, model_name, provider_name, type, version, create_time FROM model;

-- 7. 测试乐观锁更新功能
-- 模拟并发更新测试
SELECT '=== 乐观锁测试开始 ===' AS test_info;

-- 7.1 查看当前版本
SELECT id, model_id, model_name, version FROM model WHERE model_id = 'test-chat-001';

-- 7.2 模拟用户A更新成功（版本从0变成1）
UPDATE model
SET model_name = 'qwen2.5:7b-updated-by-A', version = version + 1, update_time = NOW()
WHERE model_id = 'test-chat-001' AND version = 0;

-- 7.3 检查影响行数
SELECT ROW_COUNT() AS affected_rows_A;

-- 7.4 模拟用户B更新失败（版本已经不是0了）
UPDATE model
SET model_name = 'qwen2.5:7b-updated-by-B', version = version + 1, update_time = NOW()
WHERE model_id = 'test-chat-001' AND version = 0;

-- 7.5 检查影响行数
SELECT ROW_COUNT() AS affected_rows_B;

-- 7.6 查看最终结果
SELECT id, model_id, model_name, version, update_time FROM model WHERE model_id = 'test-chat-001';

SELECT '=== 乐观锁测试完成 ===' AS test_info;