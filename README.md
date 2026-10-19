# “吃了么”外卖平台

> 面向校园与社区场景的前后端分离外卖订餐系统，覆盖普通用户、入驻商家、配送骑手、平台管理员四类角色。

[![CI](https://github.com/xiaomayo233/EatWhat/actions/workflows/ci.yml/badge.svg)](https://github.com/xiaomayo233/EatWhat/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Educational-blue.svg)](#许可)

---

## 项目简介

“吃了么”是一个完整的外卖业务闭环系统。用户可以浏览店铺与菜品、加入购物车、下单支付；商家可以维护店铺信息与菜品、接收并处理订单；骑手可以抢单配送；管理员负责资质审核、订单监管与数据看板。

本项目的技术重点在于三个问题：

1. **多角色权限隔离** —— 基于 JWT 令牌与拦截器实现接口级 RBAC（`@PublicApi` / `@RequireRole`），并在 Service 层补充数据级归属校验，防止横向越权；
2. **订单全流程状态流转** —— 用枚举约束状态机、用乐观锁保证并发安全、用状态日志留存完整轨迹，避免状态错乱；
3. **计价与库存的强一致** —— 规格加料、阶梯配送费、打包费、套餐组合价在下单时统一落快照；库存扣减走 `UPDATE ... WHERE stock >= ?` 原子语句，失败即整单回滚。

### 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.2.5、MyBatis-Plus 3.5.5、MySQL 8.0、JWT（jjwt 0.11.5）、Knife4j 4.5、spring-security-crypto（BCrypt） |
| 前端 | Vue 3、Element Plus、Pinia、Vue Router、Vite |
| 构建 | Maven、pnpm |
| 测试 | JUnit 5、Mockito、MockMvc |

### 项目规模

| 指标 | 数量 |
| --- | --- |
| 后端 Java 源文件 | 189 |
| 后端业务模块 | 15 |
| REST 端点 | 169（22 个 Controller） |
| 业务错误码 | 51 |
| 数据库表 | 27 |
| 前端页面组件 | 53 |
| 单元测试 | 163 个用例 / 13 个测试类 |

---

## 功能概览

| 角色 | 核心功能 |
| --- | --- |
| 普通用户 | 注册登录、找回密码、浏览与搜索店铺（含菜品名命中）、购物车、规格加料、下单支付、订单跟踪与实时轮询、取消订单、评价与修改、收货地址与标签、店铺收藏、退款售后、消息中心、再来一单、账号注销 |
| 入驻商家 | 资质入驻与协议签署、驳回后重提、店铺维护、按星期多营业时段、打烊提示语、菜品与分类管理（排序、批量上下架、库存预警）、规格加料、菜品套餐、接单/拒单/出餐、小票打印、评价回复、退款审核、经营分析与热销榜、店铺二维码、顾客视角预览 |
| 配送骑手 | 资质认证、抢单大厅（排序与距离/收益筛选）、接单取餐送达、接单设置（开关/半径/上限）、转单与异常上报、收入统计与逐单明细、健康证到期提醒、订单消息 |
| 平台管理员 | 管理员账号管理、用户管理、商家与骑手资质审核、订单监管与强制干预、异常订单筛查、菜品违规下架（含原因）、售后仲裁、配送异常处理、公告/敏感词/平台参数/数据字典、操作审计日志、平台统计与每日汇总 |

功能项的完整清单见 [项目功能清单](docs/09-项目功能清单.md)（221 条，已全部实现）。

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

状态迁移由 `OrderStatus.assertTransition` 统一收口，同时校验「目标状态 + 操作角色 + 前置状态」三元组：

| 目标状态 | 允许角色 | 前置状态 |
| --- | --- | --- |
| 待接单(1) | 系统（支付回调） | 待支付(0) |
| 备餐中(2) | MERCHANT | 待接单(1) |
| 待配送(3) | MERCHANT | 备餐中(2) |
| 配送中(4) | RIDER | 待配送(3) |
| 已完成(5) | RIDER / CONSUMER | 配送中(4) |
| 已取消(6) | CONSUMER / 系统 | 待支付(0) |
| 退款中(7) | MERCHANT 拒单 / CONSUMER 申诉 | 待接单(1) 或 配送中(4) |

---

## 架构设计

```
┌──────────────────────────────────────────────────────────┐
│  前端 Vue 3 SPA（127.0.0.1:5173）                          │
│  consumer / merchant / rider / admin 四端，路由守卫按角色分流  │
└───────────────────────┬──────────────────────────────────┘
                        │ HTTP /api/v1（Vite 代理）
┌───────────────────────▼──────────────────────────────────┐
│  后端 Spring Boot（127.0.0.1:8080）                        │
│                                                          │
│  TraceIdFilter → AuthInterceptor → Controller            │
│    @PublicApi 放行 / @RequireRole 校验角色                 │
│                        ↓                                 │
│  Service（事务边界 + 数据归属校验 + 业务规则）                │
│                        ↓                                 │
│  Mapper（MyBatis-Plus，复杂聚合走 @Select）                │
│                        ↓                                 │
│  Entity / MySQL 8.0（eatwhat 库，27 张表）                 │
└──────────────────────────────────────────────────────────┘
```

依赖方向严格单向：`controller → service → mapper → entity`。Service 层不出现 `HttpServletRequest`，当前用户身份由 `UserContext`（基于 ThreadLocal）传递。

### 关键设计决策

| 决策 | 取舍理由 |
| --- | --- |
| 鉴权用 `HandlerInterceptor` 而非 Spring Security 全栈 | 只需 JWT 校验与角色判断，引入完整 Security 会带来不必要的过滤器链与配置复杂度；BCrypt 单独引 `spring-security-crypto` |
| 退出登录用内存令牌黑名单（jti） | JWT 本身无状态，黑名单按 `jti → 过期时间戳` 存 `ConcurrentHashMap`，定时清理过期条目；避免为登出引入 Redis |
| 支付为模拟网关 | `POST /orders/{id}/pay` 返回 `mock://pay?...` 链接，状态变更由 `POST /orders/pay/callback` 驱动，回调幂等（重复回调不改变 `tradeNo`） |
| 库存扣减用原子 UPDATE | `UPDATE dish SET stock = stock - ? WHERE id = ? AND stock >= ?`，返回 0 行即抛库存不足，避免「先查后改」的并发超卖 |
| 订单状态变更用乐观锁 | `UPDATE orders SET status = ? WHERE id = ? AND status = ?`，返回 0 行抛状态冲突，保证并发下只有一次成功 |
| 周营业时段未配置时回落到单一时段 | `openNow()` 返回 `null` 表示未配置，此时用店铺 `openTime/closeTime` 判断，兼容旧数据 |

---

## 安全设计

| 项 | 实现 |
| --- | --- |
| 密码存储 | BCrypt（cost 10），不可逆 |
| 令牌 | JWT HS256，access 2 小时 / refresh 7 天，载荷仅 `sub`（用户 ID）、`role`、`iat`、`exp`、`jti`，不含手机号与密码 |
| 令牌失效 | 退出登录、修改密码后把当前令牌 `jti` 写入黑名单；refresh 接口同样校验黑名单 |
| 登录防爆破 | 同一手机号连续失败计数，超过阈值锁定 15 分钟并返回 `1006`；连续失败 2 次后强制图形验证码 |
| 横向越权 | Service 层逐个校验数据归属（订单、地址、菜品、评价、售后），不属于当前用户抛 `2003` |
| 图片上传 | 校验扩展名 + 文件头魔数 + 大小（≤5MB），UUID 重命名，按日期分目录 |
| 内容安全 | 评价内容与订单备注过敏感词库，命中即拒 |
| 审计 | 管理端全部写操作经 `OperationLogAspect` 记录操作人、IP、时间、方法与参数 |
| SQL 注入 | 全部走 MyBatis 参数绑定（`#{}`），无字符串拼接 |

### HTTP 状态码约定

遵循 [接口设计规范 §1.4](docs/05-接口设计规范.md)：

| 状态码 | 场景 |
| --- | --- |
| 200 | 成功，以及业务规则失败（库存不足、状态非法等，由 `code` 区分） |
| 400 | 请求参数格式错误、请求体无法解析 |
| 401 | 未登录或令牌无效（`code` 为 1004/1005/2000） |
| 403 | 已登录但无权限（`code` 为 2001/2002/2003） |
| 404 | 请求路径不存在 |
| 405 | HTTP 方法不支持 |
| 500 | 服务端内部错误 |

统一响应结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1728979200000,
  "traceId": "a1b2c3d4e5f6",
  "success": true
}
```

---

## 文档体系

设计文档位于 [`docs/`](docs/) 目录，共 9 份：

| 序号 | 文档 | 内容 |
| --- | --- | --- |
| 01 | [需求规格说明书](docs/01-需求规格说明书.md) | 业务流程、四角色功能需求、非功能需求、可行性分析 |
| 02 | [概要设计说明书](docs/02-概要设计说明书.md) | 总体架构、技术选型、模块划分、关键设计决策 |
| 03 | [详细设计说明书](docs/03-详细设计说明书.md) | 核心类设计、关键算法、安全实现、配置说明 |
| 04 | [数据库设计说明书](docs/04-数据库设计说明书.md) | E-R 模型、基础 14 张表的完整建表脚本、索引与优化 |
| 05 | [接口设计规范](docs/05-接口设计规范.md) | 接口路径、参数、响应与错误码 |
| 06 | [测试方案](docs/06-测试方案.md) | 测试用例、并发与兼容性测试、缺陷管理 |
| 07 | [项目计划与进度管理](docs/07-项目计划与进度管理.md) | WBS、六阶段进度、里程碑、风险管理 |
| 08 | [Git协作规范](docs/08-Git协作规范.md) | 分支模型、提交规范、合并与发布流程 |
| 09 | [项目功能清单](docs/09-项目功能清单.md) | 四类角色与公共支撑共 221 条功能项、非功能要求、范围边界 |

> 文档 04 描述的是需求基线中的 14 张核心表；实现过程中为支撑 09 功能清单的完整覆盖，在保持原表结构不变的前提下扩展至 27 张表（新增售后、消息、公告、敏感词、平台参数、数据字典、每日汇总、菜品规格/加料/套餐、周营业时段、配送异常、店铺收藏、操作日志等）。完整 DDL 以 [`sql/init.sql`](sql/init.sql) 为准。

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

### 数据库初始化

```bash
mysql -u root -p --default-character-set=utf8mb4 < sql/init.sql
```

脚本会重建 `eatwhat` 库、27 张表并写入种子数据（8 个用户、3 家店铺、8 个分类、20 道菜品、2 名骑手）。

### 后端启动

```bash
cd backend
# 配置环境变量（数据库密码、JWT 密钥）
export DB_USERNAME=root
export DB_PASSWORD=your_password
export JWT_SECRET=your_32_bytes_secret_at_least
mvn spring-boot:run
```

服务默认监听 `http://localhost:8080`，接口文档见 `http://localhost:8080/doc.html`（Knife4j）。

也可以先打包再运行：

```bash
cd backend
mvn clean package -DskipTests
java -Dfile.encoding=UTF-8 -jar target/eatwhat-backend.jar
```

### 前端启动

```bash
cd frontend
pnpm install
pnpm dev
```

前端默认运行在 `http://localhost:5173`，开发环境通过 Vite 代理把 `/api`、`/uploads` 转发到后端 8080 端口。

### 演示账号

| 角色 | 手机号 | 密码 |
| --- | --- | --- |
| 管理员 | 13800000000 | Admin@123 |
| 普通用户 | 13800000001 / 13800000002 | User@123 |
| 商家 | 13800000010 / 13800000011 / 13800000012 | Merchant@123 |
| 骑手 | 13800000020 / 13800000021 | Rider@123 |

> 演示店铺营业时间为 09:00–21:00。若在此时段外下单会返回 `4004 商家当前不在营业时段内`，可先用商家账号在「店铺管理」把营业时间改到当前时段。

### 测试

```bash
cd backend
mvn test        # 163 个单元测试用例
```

前端构建校验：

```bash
cd frontend
pnpm build
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
| `JWT_EXPIRATION` | 访问令牌有效期（毫秒） | 否，默认 7200000 |
| `UPLOAD_PATH` | 文件上传目录 | 否，默认 ./data/upload |

> **安全提示**：以上变量请通过环境变量或 `.env.local`（已忽略）配置，切勿提交到仓库。

---

## 项目结构

```
EatWhat/
├── .github/              # CI 流水线与 Issue/PR 模板
├── docs/                 # 设计文档（9 份）
│   └── reference/        # 学校下发的任务书、开题报告原件
├── backend/              # SpringBoot 后端工程（189 个 Java 源文件）
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/eatwhat/
│       │   ├── common/       # 统一响应、错误码、异常、常量、工具、切面
│       │   ├── security/     # JWT、令牌黑名单、登录拦截器、权限注解
│       │   └── modules/      # 15 个业务模块
│       ├── main/resources/application.yml
│       └── test/java/        # 13 个测试类，163 个用例
├── frontend/             # Vue3 前端工程（53 个页面组件）
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── api/          # 接口封装
│       ├── layouts/      # 四端布局
│       ├── router/       # 路由与角色守卫
│       ├── stores/       # Pinia 状态
│       ├── utils/        # 请求封装、格式化、状态字典
│       └── views/        # consumer/merchant/rider/admin/common 页面
├── sql/                  # init.sql：建库、27 张表、种子数据
├── .gitignore
├── .editorconfig
├── .gitattributes
├── .gitmessage
├── commitlint.config.js
├── CONTRIBUTING.md
├── CHANGELOG.md
└── README.md
```

### 后端模块划分

| 模块 | 职责 |
| --- | --- |
| `auth` | 注册、登录、刷新令牌、退出、图形验证码、找回密码、登录失败限制 |
| `user` | 个人资料（含性别生日）、密码修改、收货地址（含标签与搜索）、账号注销 |
| `shop` | 店铺列表与详情、搜索（店铺名 + 菜品名）、距离计算、周营业时段、店铺收藏 |
| `dish` | 菜品分类与菜品维护、规格加料、菜品套餐、批量上下架、库存预警 |
| `cart` | 购物车增删改查、跨店校验、规格加料计价、配送费与打包费试算、再来一单 |
| `order` | 下单、支付回调幂等、状态机流转、超时取消、小票数据、订单备注敏感词校验 |
| `delivery` | 骑手资质与审核、抢单、取餐、送达、接单设置、转单与异常上报、收入明细 |
| `review` | 订单评价、修改删除、商家回复、评分统计 |
| `aftersale` | 退款申请、商家审核、平台仲裁、售后消息推送 |
| `message` | 站内消息写入、按用户查询、未读计数、全部已读 |
| `content` | 公告、敏感词、平台参数、数据字典 |
| `merchant` | 商家入驻与审核状态、店铺维护、订单处理、经营分析、联系人维护 |
| `admin` | 用户/商家/骑手/订单/菜品监管、售后仲裁、配送异常、操作日志、平台统计 |
| `stats` | 每日经营数据汇总定时任务 |
| `file` | 图片上传与静态资源访问 |

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
