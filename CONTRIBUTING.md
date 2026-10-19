# 参与贡献

感谢你对“吃了么”外卖平台的关注。本文档说明如何参与本项目的开发与协作。

---

## 开始之前

请先阅读以下文档，了解项目全貌：

| 文档 | 内容 |
| --- | --- |
| [README.md](README.md) | 项目简介与快速开始 |
| [docs/01-需求规格说明书.md](docs/01-需求规格说明书.md) | 功能需求与业务规则 |
| [docs/02-概要设计说明书.md](docs/02-概要设计说明书.md) | 架构设计与技术选型 |
| [docs/05-接口设计规范.md](docs/05-接口设计规范.md) | 接口约定 |
| [docs/08-Git协作规范.md](docs/08-Git协作规范.md) | 分支模型与提交规范 |

---

## 开发环境

### 依赖版本

| 组件 | 版本要求 | 说明 |
| --- | --- | --- |
| JDK | 17+ | 后端编译运行 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 20+ | 前端构建 |
| pnpm | 9+ | 前端包管理 |
| MySQL | 8.0+ | 数据库 |

### 初始化步骤

```bash
# 1. 克隆仓库
git clone https://github.com/xiaomayo233/EatWhat.git
cd EatWhat

# 2. 初始化数据库
mysql -u root -p < sql/init.sql

# 3. 配置环境变量（复制示例文件后填写真实值）
copy .env.example .env.local

# 4. 启动后端
cd backend
mvn spring-boot:run

# 5. 启动前端
cd frontend
pnpm install
pnpm dev
```

> **重要**：`.env.local` 已被 `.gitignore` 排除，请勿将其提交到仓库。其中包含数据库密码与 JWT 密钥。

---

## 分支模型

```
main        ← 生产就绪代码，受保护
  └ develop ← 集成分支，受保护
      ├ feature/xxx  功能开发
      ├ fix/xxx      缺陷修复
      └ docs/xxx     文档变更
```

**流程**：

```bash
# 1. 从 develop 切出功能分支
git checkout develop
git pull --rebase origin develop
git checkout -b feature/your-feature

# 2. 小步提交（遵循 Conventional Commits）
git commit -m "feat(order): 实现订单创建接口"

# 3. 推送并创建 PR
git push -u origin feature/your-feature

# 4. 合并后删除分支
git branch -d feature/your-feature
```

分支命名与提交规范详见 [docs/08-Git协作规范.md](docs/08-Git协作规范.md)。

---

## 提交信息规范

格式：`<type>(<scope>): <subject>`

```bash
# 正确的提交
git commit -m "feat(cart): 实现跨店加购校验"
git commit -m "fix(auth): 修复令牌过期后未清除缓存"
git commit -m "docs(api): 补充订单状态迁移矩阵"

# 会被拒绝的提交
git commit -m "update"          # 缺少 type
git commit -m "修复bug"          # 使用中文，缺少 type
git commit -m "WIP"             # 无意义
```

**可选 type**：`feat` `fix` `docs` `style` `refactor` `perf` `test` `build` `ci` `chore` `revert`

**推荐 scope**：`auth` `user` `shop` `dish` `cart` `order` `delivery` `review` `admin` `common` `db` `docs`

---

## 代码规范

### 后端（Java）

| 项 | 要求 |
| --- | --- |
| 命名 | 类名大驼峰，方法名小驼峰，常量全大写下划线 |
| 分层 | Controller 只做参数校验与响应封装，业务逻辑全部在 Service |
| 异常 | 业务失败抛 `BizException`，禁止吞异常、禁止 `e.printStackTrace()` |
| 日志 | 使用 SLF4J，禁止字符串拼接，敏感信息脱敏 |
| SQL | 禁止 `SELECT *`，禁止字符串拼接 SQL |
| 事务 | 多表写操作必须加 `@Transactional(rollbackFor = Exception.class)` |

### 前端（Vue3）

| 项 | 要求 |
| --- | --- |
| 组件名 | 大驼峰，多单词 |
| 组合式 API | 统一使用 `<script setup>` |
| 状态管理 | 跨组件共享状态使用 Pinia |
| 请求 | 统一走 `src/utils/request.js`，禁止组件内直接 `axios` |
| 样式 | 使用 scoped 或 BEM 命名，禁止全局污染 |

### 通用

- 缩进与换行遵循 `.editorconfig`；
- 提交前确保 `.idea/`、`node_modules/`、`target/` 未被暂存；
- 敏感配置一律通过环境变量注入。

---

## 提交前自检

```bash
# 后端编译
cd backend && mvn clean compile

# 前端构建
cd frontend && pnpm build

# 检查暂存内容，确认无敏感信息
git diff --staged
```

**必须确认**：

- [ ] 代码可编译
- [ ] 无硬编码密码、密钥、内网 IP
- [ ] 未提交 `.env`、`.idea/`、`target/`、`node_modules/`
- [ ] 新增接口已同步到 `docs/05-接口设计规范.md`
- [ ] 写接口已校验数据归属

---

## 提交缺陷

请使用 [Bug 报告模板](.github/ISSUE_TEMPLATE/bug_report.md)，包含复现步骤、实际结果、预期结果与环境信息。缺少复现步骤的问题难以定位，可能被延迟处理。

## 提交需求

请使用 [功能需求模板](.github/ISSUE_TEMPLATE/feature_request.md)，说明需求背景与验收标准。

---

## 文档贡献

文档同样纳入版本管理，修改文档请：

1. 使用 `docs` 类型的提交；
2. 同步更新文档头部的版本号与修订记录；
3. 保持章节编号与目录一致。

---

## 许可

本项目为毕业设计作品，仅用于学习与教学目的。
