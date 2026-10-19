# “吃了么”外卖平台

> 面向校园与社区场景的前后端分离外卖订餐系统，覆盖普通用户、入驻商家、配送骑手、平台管理员四类角色。

[![CI](https://github.com/xiaomayo233/EatWhat/actions/workflows/ci.yml/badge.svg)](https://github.com/xiaomayo233/EatWhat/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Educational-blue.svg)](#许可)

---

## 项目简介

“吃了么”是一个完整的外卖业务闭环系统。用户可以浏览店铺与菜品、加入购物车、下单支付；商家可以维护店铺信息与菜品、接收并处理订单；骑手可以抢单配送；管理员负责资质审核、订单监管与数据看板。

本项目的技术重点在于两个问题：

1. **多角色权限隔离** —— 基于 Spring Security 实现接口级 RBAC，并在 Service 层补充数据级归属校验，防止横向越权；
2. **订单全流程状态流转** —— 用枚举约束状态机、用乐观锁保证并发安全、用状态日志留存完整轨迹，避免状态错乱。

### 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.2、Spring Security 6、MyBatis-Plus、MySQL 8.0 |
| 前端 | Vue 3、Element Plus、Pinia、Vue Router、Vite |
| 构建 | Maven、pnpm |

---

## 功能概览

| 角色 | 核心功能 |
| --- | --- |
| 普通用户 | 注册登录、浏览店铺与菜品、购物车、下单支付、订单跟踪、售后、评价、消息 |
| 入驻商家 | 资质入驻、店铺维护、菜品与分类管理、库存、上下架、接单/拒单/出餐、售后、经营统计 |
| 配送骑手 | 资质认证、接单设置、抢单、取餐、送达、收入统计、消息 |
| 平台管理员 | 用户管理、商家与骑手资质审核、订单监管与退款裁定、菜品监管、内容管理、数据看板 |

功能项的完整清单见 [项目功能清单](docs/09-项目功能清单.md)。

### 订单状态机

```
待支付(0) ──支付──> 待接单(1) ──接单──> 备餐中(2) ──出餐──> 待配送(3)
   │                    │                                        │
   │取消                 │拒单                              ──抢单──┤
   ▼                    ▼                                        ▼
已取消(6)          退款中(7)                              配送中(4)
                                                               │
                                                          完成 ▼
                                                          已完成(5)
```

---

## 文档体系

设计文档位于 [`docs/`](docs/) 目录，共 9 份：

| 序号 | 文档 | 内容 |
| --- | --- | --- |
| 01 | [需求规格说明书](docs/01-需求规格说明书.md) | 业务流程、四角色功能需求、非功能需求、可行性分析 |
| 02 | [概要设计说明书](docs/02-概要设计说明书.md) | 总体架构、技术选型、模块划分、关键设计决策 |
| 03 | [详细设计说明书](docs/03-详细设计说明书.md) | 核心类设计、关键算法、安全实现、配置说明 |
| 04 | [数据库设计说明书](docs/04-数据库设计说明书.md) | E-R 模型、14 张表的完整建表脚本、索引与优化 |
| 05 | [接口设计规范](docs/05-接口设计规范.md) | 74 个接口的路径、参数、响应与错误码 |
| 06 | [测试方案](docs/06-测试方案.md) | 96 条测试用例、并发与兼容性测试、缺陷管理 |
| 07 | [项目计划与进度管理](docs/07-项目计划与进度管理.md) | WBS、六阶段进度、里程碑、风险管理 |
| 08 | [Git协作规范](docs/08-Git协作规范.md) | 分支模型、提交规范、合并与发布流程 |
| 09 | [项目功能清单](docs/09-项目功能清单.md) | 四类角色与公共支撑共 221 条功能项、非功能要求、范围边界 |

---

## 快速开始

### 环境要求

| 组件 | 版本 |
| --- | --- |
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 20+ |
| pnpm | 9+ |
| MySQL | 8.0+ |

### 后端启动

```bash
cd backend
# 配置环境变量（数据库密码、JWT 密钥）
export DB_USERNAME=eatwhat_app
export DB_PASSWORD=your_password
export JWT_SECRET=your_32_bytes_secret_at_least
mvn spring-boot:run
```

服务默认监听 `http://localhost:8080`，接口文档见 `http://localhost:8080/doc.html`。

### 前端启动

```bash
cd frontend
pnpm install
pnpm dev
```

前端默认运行在 `http://localhost:5173`。

### 数据库初始化

```bash
mysql -u root -p < sql/init.sql
```

---

## 环境变量

| 变量 | 说明 | 是否必填 |
| --- | --- | --- |
| `DB_HOST` | 数据库主机 | 否，默认 localhost |
| `DB_PORT` | 数据库端口 | 否，默认 3306 |
| `DB_NAME` | 数据库名 | 否，默认 eatwhat |
| `DB_USERNAME` | 数据库用户名 | 是 |
| `DB_PASSWORD` | 数据库密码 | 是 |
| `JWT_SECRET` | JWT 签名密钥（≥32 字节） | 是 |
| `UPLOAD_PATH` | 文件上传目录 | 否，默认 ./data/upload |

> **安全提示**：以上变量请通过环境变量或 `.env.local`（已忽略）配置，切勿提交到仓库。

---

## 项目结构

当前仓库已完成设计阶段交付物，`backend/`、`frontend/`、`sql/` 三个目录将按
[项目计划](docs/07-项目计划与进度管理.md) 的阶段二至阶段三逐步建立。

```
EatWhat/
├── .github/              # CI 流水线与 Issue/PR 模板
├── docs/                 # 设计文档（9 份）
│   └── reference/        # 学校下发的任务书、开题报告原件
├── backend/              # （待建）SpringBoot 后端工程
│   └── src/main/java/com/eatwhat/
│       ├── common/       # 通用：响应、异常、配置
│       ├── security/     # 认证授权
│       └── modules/      # 业务模块
├── frontend/             # （待建）Vue3 前端工程
│   └── src/
│       ├── api/          # 接口封装
│       ├── views/        # 页面
│       ├── stores/       # Pinia 状态
│       └── utils/        # 工具
├── sql/                  # （待建）建库建表脚本
├── .gitignore
├── .editorconfig
├── .gitattributes
├── .gitmessage
├── commitlint.config.js
├── CONTRIBUTING.md
├── CHANGELOG.md
└── README.md
```

> **说明**：数据库建表脚本目前以完整 SQL 形式内嵌在
> [数据库设计说明书](docs/04-数据库设计说明书.md) 中，编码阶段将抽取为
> `sql/init.sql` 供自动化初始化使用。

---

## 开发规范

本项目遵循以下规范，详见 [CONTRIBUTING.md](CONTRIBUTING.md) 与 [Git 协作规范](docs/08-Git协作规范.md)：

- **分支模型**：`main` ← `develop` ← `feature/*`，合并使用 `--no-ff`
- **提交规范**：Conventional Commits，格式 `<type>(<scope>): <subject>`
- **代码规范**：分层职责清晰，权限与事务不可遗漏
- **文档同步**：接口变更必须同步更新接口文档

```bash
# 提交示例
git commit -m "feat(order): 实现订单创建与库存原子扣减"
git commit -m "fix(auth): 修复令牌过期后未清除本地缓存"
git commit -m "docs(api): 补充订单状态迁移矩阵"
```

---

## 许可

本项目为电子科技大学中山学院软件工程专业毕业设计作品，仅用于学习与教学目的。

---

## 致谢

感谢指导教师李文生教授的指导，感谢开源社区提供的优秀框架与工具。
