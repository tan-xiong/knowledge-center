# 🧠 Knowledge Center - 智能知识中心

<div align="center">
  
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-brightgreen)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-success)
![Java](https://img.shields.io/badge/Java-21-orange)
![Milvus](https://img.shields.io/badge/Milvus-Vector%20DB-blue)
![Redis](https://img.shields.io/badge/Redis-Cache-red)
![License](https://img.shields.io/badge/License-MIT-yellow)

*基于Spring AI的企业级智能知识管理系统*

</div>

## 🎯 项目简介

Knowledge Center是一个基于**Spring AI**和**RAG（检索增强生成）**技术的智能知识管理系统。通过集成阿里云通义千问大模型、Milvus向量数据库和Redis缓存，实现了：

- 📚 **智能文档管理**：自动向量化存储各类文档
- 🤖 **智能问答对话**：基于RAG的上下文感知问答
- 🧠 **记忆式对话**：支持多轮连续对话记忆
- 🔍 **语义搜索**：基于向量相似度的智能检索
- 🌊 **流式响应**：实时流式输出，提升用户体验


## 🚀 快速开始

### 环境要求

- Java 21+
- Maven 3.6+
- Redis 6.0+
- Milvus 2.0+

### 1. 克隆项目

```bash
git clone https://github.com/your-org/knowledge-center.git
cd knowledge-center
```


## 📚 功能特性

### 🤖 智能对话

- **流式响应**：实时输出，提升用户体验
- **上下文记忆**：支持多轮连续对话
- **RAG增强**：基于知识库的精准问答
- **个性化**：每个对话ID独立的记忆空间


### 📄 文档管理

- **多格式支持**：支持TXT、PDF、DOC、DOCX等多种文档格式
- **智能处理**：自动识别文档类型并选择合适的解析器
- **批量处理**：支持并行处理多个文档片段
- **智能切分**：自动按段落切分文档内容，优化检索效果
- **向量化存储**：文档内容自动转换为向量存储到Milvus
- **语义检索**：基于向量相似度的智能搜索（相似度阈值可配置）
- **文件管理**：支持10MB以内的文件上传，自动元数据管理


### 🔍 高级检索

- **相似度阈值**：可配置的相似度阈值过滤
- **TopK检索**：返回最相关的K个结果
- **多维度搜索**：支持标题、内容、标签等多维度检索


## 🚀 功能规划
- [✅] 基础智能对话系统
- [✅] RAG增强问答
- [✅] 对话记忆管理
- [✅] 多格式文档上传（PDF、TXT、DOC、DOCX）
- [✅] 智能文档切分
- [✅] 语义搜索
- [✅] 流式响应
- [✅] 统一API响应格式
- [✅] 知识库管理
- [✅] 支持多窗口对话
- [✅] 聊天数据持久化


## 🤝 贡献指南

1. Fork项目
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建Pull Request

## 📄 许可证

本项目基于MIT许可证开源。详见 [LICENSE](LICENSE) 文件。