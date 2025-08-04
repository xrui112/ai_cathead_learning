# AI CatHead Learning Platform

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-blue.svg)](https://spring.io/projects/spring-ai)

## 📋 项目简介

基于领域驱动设计(DDD)架构的现代化AI学习平台，集成多模型供应商管理。项目采用整洁架构原则，实现高内聚、低耦合的模块化设计。

## 分支管理

master 主开发分支



## 🏗️ 架构概览
ai-rag-catheadLearning/  

├── cathead-sy-trigger/        # 触发器层(Controller) - 接口入口  

├── cathead-sy-app/          # 应用服务层 - 用例编排  


├── cathead-sy-domain/       # 领域核心层 - 业务逻辑  

├── cathead-sy-infrastructure/ # 基础设施层 - 数据持久化  

└── cathead-sy-types/        # 类型定义层 - 通用类型  




### 🎯 DDD分层架构

| 层级 | 职责 | 包含模块 |
|------|------|----------|
| **Trigger Layer** | HTTP接口、事件触发 | `cathead-sy-trigger` |
| **Application Layer** | 应用服务、用例编排 | `cathead-sy-app` |  
| **Domain Layer** | 业务逻辑、领域模型 | `cathead-sy-domain` |
| **Infrastructure Layer** | 数据持久化、外部集成 | `cathead-sy-infrastructure` |
| **Types Layer** | 通用类型、枚举 | `cathead-sy-types` |

## 🧩 领域模型设计

### 🤖 Model领域 - 多模型供应商统一管理

Model领域作为系统核心，负责管理不同AI模型供应商的统一接入和配置管理：

#### 📦 子领域划分

##### 🔧 ModelBean子领域
```java
cn.cathead.ai.domain.model.service.modelcache/
├── IModelBeanManager.java          # ModelBean管理接口
└── ModelBeanImpl/
    └── ModelBeanManager.java       # ModelBean生命周期管理
```

**核心职责:**
- 🚀 ModelBean的创建、删除、更新、获取
- 🔄 模型实例生命周期管理
- 💾 模型Bean缓存策略
- ⚡ 动态模型切换支持

##### 🏭 Provider子领域
```java
cn.cathead.ai.domain.model.service.provider/
├── IModelProvider.java             # 模型提供商接口
└── providerImpl/
    ├── OllamaProviderI.java       # Ollama供应商实现
    └── OpenaiProviderI.java       # OpenAI供应商实现
```

**核心职责:**
- 🌐 统一多供应商接入协议
- 🔌 策略模式支持供应商扩展
- ⚙️ 供应商特定配置管理
- 🛡️ 供应商连接池与熔断

**支持的供应商:**
- ✅ **Ollama** - 本地化模型服务
- ✅ **OpenAI** - 商业化API服务
- 🔄 **扩展中** - Anthropic, Google, Azure等

##### 📋 Form子领域
```java
cn.cathead.ai.domain.model.form/
├── config/
│   ├── FormConfigurationManager.java  # 表单配置管理
│   └── FieldDefinition.java          # 字段定义模型
├── validation/
│   ├── DynamicFormValidator.java     # 动态校验引擎
│   └── CustomValidator.java         # 自定义校验器
└── service/
    └── FormService.java              # 表单服务编排
```

**核心职责:**
- 📝 动态表单配置管理
- ✅ 智能校验规则编排
- 🎯 供应商差异化字段处理
- 🔧 可扩展字段验证框架

**动态表单特性:**
- 🎛️ 基于provider+type的智能字段显示
- 📏 多层级校验规则(必填、格式、范围、自定义)
- 🔄 配置驱动的字段扩展
- 💡 用户友好的错误提示


## 📊 技术栈

### 后端核心
- **Java 17** - 现代Java特性支持
- **Spring Boot 3.2.3** - 企业级应用框架
- **Spring AI 1.0.0** - AI应用开发框架
- **MyBatis** - 持久层框架
- **MySQL** - 关系型数据库
- **PgVector** - 向量数据库
- **Redis** - 缓存和会话管理

## 🚀 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Docker (可选)

### 安装部署

1. **克隆项目**
```bash
git clone https://github.com/your-username/ai-rag-catheadLearning.git
cd ai-rag-catheadLearning
```

2. **环境配置**
```bash
# 复制配置文件
cp cathead-sy-app/src/main/resources/application-dev.yml.example \
   cathead-sy-app/src/main/resources/application-dev.yml

# 修改数据库连接配置
vim cathead-sy-app/src/main/resources/application-dev.yml
```

3. **启动依赖服务**
```bash
# 使用Docker Compose启动基础服务
cd doc/dockeryml/ollama
docker-compose up -d
```

4. **编译运行**
```bash
# 编译项目
mvn clean compile

# 启动应用
cd cathead-sy-app
mvn spring-boot:run
```


## 📚 API文档

### 模型管理 API

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/model/create` | POST | 创建模型配置 |
| `/api/model/{id}` | PUT | 更新模型配置 |
| `/api/model/{id}` | DELETE | 删除模型配置 |
| `/api/model/{id}` | GET | 查询模型详情 |

### 动态表单 API

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/model-form/config` | GET | 获取表单配置 |
| `/api/model-form/validate` | POST | 校验表单数据 |


## 🤝 贡献指南

欢迎参与项目贡献！请遵循以下步骤：

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。


⭐ 如果这个项目对你有帮助，请给一个Star支持！
