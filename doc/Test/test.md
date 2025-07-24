# AI模型管理系统

## 基础配置

- http://localhost:8090
- application/json

---

## 一、模型管理接口 (`/api/v1/manage`)

### 1.1 创建Chat模型

**接口**: `POST /api/v1/manage/create_chat`

**请求参数** (form-data):

```
providerName: ollama
modelName: qwen3
url: http://localhost:11434
key: 
type: chat
temperature: 0.7
topP: 0.9
maxTokens: 2048
presencePenalty: 0.0
frequencyPenalty: 0.0
```

### 1.2 创建Embedding模型

**接口**: `POST /api/v1/manage/create_embedding`

**请求参数** (form-data):

```
providerName: ollama
modelName: nomic-embed-text
url: http://localhost:11434
key: 
type: embedding
embeddingFormat: json
numPredict: 512
```

### 1.3 更新Chat模型配置

**接口**: `PUT /api/v1/manage/chat/{modelId}`

**路径参数**:

- `modelId`: 模型ID (例如: `test-model-123`)

**请求体** (JSON):

```json
{
    "providerName": "ollama",
    "modelName": "qwen3-updated",
    "url": "http://localhost:11434",
    "key": "",
    "type": "chat",
    "temperature": 0.8,
    "topP": 0.95,
    "maxTokens": 4096,
    "presencePenalty": 0.1,
    "frequencyPenalty": 0.1
}
```

### 1.4 更新Embedding模型配置

**接口**: `PUT /api/v1/manage/embedding/{modelId}`

**路径参数**:

- `modelId`: 模型ID

**请求体** (JSON):

```json
{
    "providerName": "ollama",
    "modelName": "nomic-embed-text-v2",
    "url": "http://localhost:11434",
    "key": "",
    "type": "embedding",
    "embeddingFormat": "binary",
    "numPredict": 1024
}
```

### 1.5 删除模型

**接口**: `DELETE /api/v1/manage/model/{modelId}`

**路径参数**:

- `modelId`: 要删除的模型ID

### 1.6 获取模型信息

**接口**: `GET /api/v1/manage/model/{modelId}`

**路径参数**:

- `modelId`: 模型ID

### 1.7 刷新模型缓存

**接口**: `POST /api/v1/manage/model/{modelId}/refresh`

**路径参数**:

- `modelId`: 模型ID

### 1.8 批量刷新所有模型缓存

**接口**: `POST /api/v1/manage/model/refresh/all`

**无参数**

### 1.9 获取Bean管理统计信息

**接口**: `GET /api/v1/manage/model/bean/stats`

**无参数**

### 1.10 清空所有模型Bean

**接口**: `POST /api/v1/manage/model/bean/clear`

**无参数**

---

## 二、动态表单接口 (`/api/v1/manage/model_form`)

### 2.1 获取动态表单配置

**接口**: `GET /api/v1/manage/model_form/config`

**查询参数**:

- `provider`: ollama
- `type`: chat

**测试用例**:

1. **Chat模型配置**: `?provider=ollama&type=chat`
2. **Embedding模型配置**: `?provider=ollama&type=embedding`
3. **不支持的配置**: `?provider=unsupported&type=unknown`

### 2.2 校验动态表单数据

**接口**: `POST /api/v1/manage/model_form/validate`

**查询参数**:

- `provider`: ollama
- `type`: chat

**请求体** (JSON) - 有效数据:

```json
{
    "modelName": "qwen3",
    "temperature": 0.7,
    "url": "http://localhost:11434",
    "maxTokens": 2048
}
```

**请求体** (JSON) - 无效数据测试:

```json
{
    "modelName": "",
    "temperature": 5.0,
    "url": "invalid-url",
    "maxTokens": -100
}
```

### 2.3 提交动态表单并创建模型

**接口**: `POST /api/v1/manage/model_form/submit`

**查询参数**:

- `provider`: ollama
- `type`: chat

**请求体** (JSON):

```json
{
    "modelName": "qwen3-dynamic",
    "temperature": 0.7,
    "url": "http://localhost:11434",
    "maxTokens": 2048,
    "topP": 0.9
}
```

---

## 三、模型服务接口 (`/api/v1/service`)

### 3.1 调用Chat模型

**接口**: `POST /api/v1/service/chat_with`

**请求参数** (form-data):

```
modelId: test-model-123
prompt: 你好，请介绍一下自己
```

**测试用例**:

1. **正常对话**:
   - `prompt`: "你好，请介绍一下自己"
2. **长文本对话**:
   - `prompt`: "请详细解释什么是人工智能，包括其发展历史、主要技术和应用领域"
3. **代码相关**:
   - `prompt`: "用Python写一个快速排序算法"
4. **不存在的模型**:
   - `modelId`: "non-existent-model"

---
