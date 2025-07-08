基于DDD的AI学习项目 
涉及多模型供应商管理,MCP,RAG,Agent
功能基于领域构建

Model领域 以下是子领域的划分
  -ModelBean领域 封装了modelBean的创建,删除,更改,获取 逻辑
  -provider领域 负责各个模型厂商的provider生成,modelbean领域将会使用不同的provider来生成modelbean
  -form领域 模型涉及很多配置and配置校验,这个领域会负责解决模型配置动态表单需求and编排校验规则
