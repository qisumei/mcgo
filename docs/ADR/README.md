# Architecture Decision Records (ADR)

本目录包含 MCGO 项目的架构决策记录。

## 什么是 ADR？

架构决策记录（Architecture Decision Records，ADR）是记录重要架构决策及其背景和后果的文档。

## ADR 格式

每个 ADR 应包含以下部分：

1. **标题**: ADR 编号和简短标题
2. **状态**: 提议、已接受、已废弃、已替代
3. **上下文**: 做出决策的背景和原因
4. **决策**: 具体的架构决策
5. **后果**: 决策带来的影响（正面和负面）
6. **备选方案**: 考虑过的其他选项

## 命名规范

ADR 文件命名格式：`NNNN-title-in-kebab-case.md`

例如：
- `0001-multi-module-architecture.md`
- `0002-ports-and-adapters-pattern.md`
- `0003-event-driven-architecture.md`

## 现有 ADR

### [ADR-0001: 多模块架构迁移](0001-multi-module-architecture.md)

**状态**: 已接受  
**日期**: 2024-10  
**决策**: 将单体项目重构为多模块 Gradle 项目，采用端口-适配器架构模式

---

## 创建新 ADR

当需要记录重要架构决策时：

1. 创建新的 ADR 文件（使用下一个编号）
2. 使用上述格式填写内容
3. 在本 README 中添加索引
4. 提交 PR 供团队审查

## 参考资源

- [ADR GitHub Organization](https://adr.github.io/)
- [Documenting Architecture Decisions](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
