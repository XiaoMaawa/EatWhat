# “吃了么”外卖平台 Git 协作规范

| 项目 | 内容 |
| --- | --- |
| 文档编号 | EW-GIT-008 |
| 项目名称 | “吃了么”外卖平台 |
| 文档版本 | V1.0 |
| 编写人 | 马乾罡 |
| 编写日期 | 2026-10 |
| 远端仓库 | https://github.com/xiaomayo233/EatWhat.git |

---

## 1 总则

### 1.1 目的

版本管理不仅是"备份代码"，更是工程能力的体现。本文档定义本项目从分支模型、提交信息、代码评审到发布打标的完整规范，确保提交历史清晰可追溯、每次变更可回滚、协作过程可审计。

### 1.2 基本原则

| 原则 | 说明 |
| --- | --- |
| 一次提交只做一件事 | 提交粒度以"可独立回滚"为标准，禁止把功能代码与格式调整混在一次提交 |
| 提交信息说明"为什么" | 标题写"做了什么"，正文写"为什么这么做" |
| 主分支永远可运行 | `main` 上任何一次提交都应处于可编译、可启动状态 |
| 变更必须经分支 | 禁止直接在 `main` 上开发，所有改动经由 `feature/*` 或 `hotfix/*` 分支 |
| 敏感信息不入库 | 密钥、密码、证书、本地配置一律通过环境变量注入 |

---

## 2 分支模型

### 2.1 分支总览

本项目采用 Git Flow 的简化版本，适用于单人开发但保留团队协作的完整骨架。

| 分支 | 用途 | 生命周期 | 保护 |
| --- | --- | --- | --- |
| `main` | 生产就绪代码，每次提交对应一个可交付版本 | 永久 | 受保护，禁止直接推送 |
| `develop` | 集成分支，日常开发的汇总 | 永久 | 受保护，禁止直接推送 |
| `feature/*` | 单个功能开发 | 功能合入后删除 | 无 |
| `hotfix/*` | 生产缺陷紧急修复 | 修复后删除 | 无 |
| `release/*` | 版本发布准备（可选，毕业设计阶段可简化） | 发布后删除 | 无 |
| `docs/*` | 纯文档变更 | 合入后删除 | 无 |

### 2.2 分支流转

```
main ─────●─────────────────●──────────●─────────►
          │                 │          │
          │            (merge)    (merge)
          │                 │          │
develop ──●────●────●───────●────●─────●─────────►
               │    │            │
          feature  feature    hotfix
           /auth   /order     /pay-bug
```

**流转规则**：

1. `feature/*` 从 `develop` 切出，完成后合回 `develop`；
2. `hotfix/*` 从 `main` 切出，修复后同时合回 `main` 与 `develop`；
3. `develop` 在里程碑节点合入 `main`，并打版本标签；
4. 分支合并一律使用 `--no-ff`，保留分支拓扑。

### 2.3 分支命名规范

```
<type>/<short-description>
```

| type | 含义 | 示例 |
| --- | --- | --- |
| `feature` | 新功能 | `feature/user-auth` |
| `fix` | 缺陷修复（非紧急） | `fix/cart-quantity` |
| `hotfix` | 生产紧急修复 | `hotfix/pay-callback` |
| `docs` | 文档 | `docs/api-spec` |
| `refactor` | 重构 | `refactor/order-service` |
| `test` | 测试相关 | `test/order-cases` |
| `chore` | 构建、依赖、配置 | `chore/upgrade-springboot` |

**命名要求**：

- 全小写，单词间用连字符 `-`；
- 描述使用英文，2—4 个单词，能看出改动范围；
- 禁止使用 `test`、`tmp`、`mybranch`、`123` 等无意义名称；
- 禁止使用中文、空格、下划线。

---

## 3 提交规范（Conventional Commits）

### 3.1 提交信息结构

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

| 部分 | 必填 | 说明 |
| --- | --- | --- |
| `type` | 是 | 提交类型 |
| `scope` | 否 | 影响范围，如 `order`、`auth`、`cart` |
| `subject` | 是 | 简短描述，不超过 50 字符，不以句号结尾 |
| `body` | 否 | 详细说明，每行不超过 72 字符 |
| `footer` | 否 | 关联 Issue、破坏性变更说明 |

### 3.2 提交类型

| type | 含义 | 触发场景 |
| --- | --- | --- |
| `feat` | 新增功能 | 实现一个新接口、新页面 |
| `fix` | 修复缺陷 | 修正错误逻辑 |
| `docs` | 文档变更 | 修改设计文档、README |
| `style` | 代码格式 | 缩进、分号、空格，不影响逻辑 |
| `refactor` | 重构 | 既不新增功能也不修复缺陷的代码调整 |
| `perf` | 性能优化 | 提升查询速度、减少渲染开销 |
| `test` | 测试相关 | 新增或修改测试用例 |
| `build` | 构建系统 | Maven、Vite 配置变更 |
| `ci` | 持续集成 | GitHub Actions 配置变更 |
| `chore` | 杂项 | 依赖升级、忽略规则调整 |
| `revert` | 回滚 | 撤销此前的提交 |

### 3.3 scope 取值

| scope | 对应模块 |
| --- | --- |
| `auth` | 认证授权 |
| `user` | 用户与地址 |
| `shop` | 店铺 |
| `dish` | 菜品 |
| `cart` | 购物车 |
| `order` | 订单 |
| `delivery` | 配送 |
| `review` | 评价 |
| `admin` | 管理员后台 |
| `common` | 通用组件 |
| `db` | 数据库脚本 |
| `docs` | 文档 |
| `deps` | 依赖 |

### 3.4 正确与错误示例

**正确**：

```
feat(order): 实现订单创建与库存原子扣减

订单创建涉及订单主表、明细表、库存、购物车、状态日志五处写操作，
必须在同一事务内完成。库存扣减采用条件更新避免超卖：

  UPDATE dish SET stock = stock - ? WHERE id = ? AND stock >= ?

这样判断与更新在同一条语句内完成，由数据库行锁保证原子性。

Refs: #12
```

```
fix(auth): 修复令牌过期后未清除本地缓存的问题

原实现在响应码 1005 时仅提示错误，未清除 localStorage 中的令牌，
导致用户反复被拦截在登录页。现在统一调用 userStore.logout()。
```

```
docs(readme): 补充本地启动的环境变量说明
```

**错误**：

| 错误示例 | 问题 |
| --- | --- |
| `update` | 缺少 type |
| `修复了一个bug` | 使用中文，缺少 type |
| `feat: 修改了很多东西` | 描述空泛，无法判断改动范围 |
| `feat(order): 实现订单功能。` | 标题以句号结尾 |
| `feat(order): 实现订单创建、支付、配送、评价、后台管理全部功能` | 标题过长且一次做太多事 |
| `WIP` | 无意义 |

### 3.5 破坏性变更

若提交包含不兼容变更，在 type 后加 `!`，并在 footer 写明 `BREAKING CHANGE`：

```
feat(api)!: 统一响应结构改为 code/message/data

BREAKING CHANGE: 原 `success` 布尔字段移除，前端需改用 `code === 0` 判断。
所有接口响应结构同步调整。
```

### 3.6 提交粒度

| 场景 | 做法 |
| --- | --- |
| 实现一个接口 | 一次提交（Controller + Service + Mapper + DTO） |
| 修复一个缺陷 | 一次提交 |
| 格式调整 | 单独一次提交，type 为 `style` |
| 依赖升级 | 单独一次提交，type 为 `build` 或 `chore` |
| 文档更新 | 单独一次提交，type 为 `docs` |

**禁止**：

- 一次提交修改 20 个不相关文件；
- 把 `.idea/`、`node_modules/`、编译产物提交入库；
- 提交信息为 `1`、`2`、`a`、`123`、`.`、`update`、`fix` 等。

---

## 4 合并与评审

### 4.1 合并策略

| 场景 | 策略 | 命令 |
| --- | --- | --- |
| feature → develop | `--no-ff` 合并，保留分支记录 | `git merge --no-ff feature/user-auth` |
| hotfix → main | `--no-ff` 合并 | `git merge --no-ff hotfix/pay-callback` |
| develop → main（发布） | `--no-ff` 合并 | `git merge --no-ff develop` |
| 同步远端更新 | rebase | `git pull --rebase origin develop` |

**为什么用 `--no-ff`**：快进合并会让分支历史变成一条直线，无法看出"哪些提交属于同一个功能"。`--no-ff` 强制生成合并提交，历史上能清晰看到一个功能的起止边界，便于日后回溯与回滚。

### 4.2 合并前检查清单

| 检查项 | 说明 |
| --- | --- |
| 编译通过 | `mvn clean compile` 无错误 |
| 代码规范 | 无未使用的 import、无 `System.out.println` |
| 敏感信息 | 无硬编码密码、密钥、IP |
| 无关文件 | 未包含 `.idea/`、`target/`、`node_modules/` |
| 提交信息 | 符合 Conventional Commits |
| 文档同步 | 接口变更已更新接口文档 |

### 4.3 Pull Request 规范

**标题**：与提交信息格式一致，如 `feat(order): 实现订单创建与状态流转`

**描述模板**：

```markdown
## 变更类型
- [ ] 新功能 feat
- [ ] 缺陷修复 fix
- [ ] 文档 docs
- [ ] 重构 refactor
- [ ] 其他

## 变更内容
<!-- 简要说明做了什么 -->

## 关联 Issue
Closes #12

## 自测情况
- [ ] 接口可正常调用
- [ ] 前端页面可正常操作
- [ ] 权限校验生效
- [ ] 已更新相关文档

## 影响范围
<!-- 是否影响其他模块 -->
```

### 4.4 单人项目的评审替代方案

本项目为单人开发，无法进行真正的同行评审。为保证质量，采取以下替代措施：

1. **PR 自查**：即使自己合并，也创建 PR，强制走一遍检查清单；
2. **延迟合并**：PR 创建后至少间隔 2 小时再合并，留出复看时间；
3. **自审重点**：权限校验、事务边界、空指针、SQL 注入四类问题必查；
4. **CI 兜底**：通过 GitHub Actions 自动编译与静态检查。

---

## 5 版本发布

### 5.1 语义化版本

采用 [Semantic Versioning 2.0.0](https://semver.org/lang/zh-CN/)：`MAJOR.MINOR.PATCH`

| 位 | 递增条件 |
| --- | --- |
| MAJOR | 不兼容的接口变更 |
| MINOR | 向下兼容的功能新增 |
| PATCH | 向下兼容的缺陷修复 |

### 5.2 版本规划

| 版本 | 对应阶段 | 内容 |
| --- | --- | --- |
| `v0.1.0` | 阶段二 | 基础框架 + 认证模块 |
| `v0.2.0` | 阶段二 | 店铺菜品 + 购物车 |
| `v0.3.0` | 阶段三 | 订单 + 支付 |
| `v0.4.0` | 阶段三 | 配送 + 评价 |
| `v0.5.0` | 阶段三 | 管理员后台 |
| `v1.0.0` | 阶段四 | 功能完整、测试通过 |

### 5.3 标签规范

**附注标签**（推荐）：

```bash
git tag -a v1.0.0 -m "release: v1.0.0

- 四端功能完整
- 测试用例通过率 99.0%
- 无致命与严重缺陷"
git push origin v1.0.0
```

**标签说明必须包含**：版本亮点、测试结论、已知问题。

---

## 6 日常操作规范

### 6.1 开始新功能

```bash
# 1. 切换到 develop 并同步远端
git checkout develop
git pull --rebase origin develop

# 2. 创建功能分支
git checkout -b feature/user-auth

# 3. 开发过程中小步提交
git add src/main/java/com/eatwhat/modules/auth/
git commit -m "feat(auth): 实现登录接口与 JWT 签发"

# 4. 推送分支
git push -u origin feature/user-auth
```

### 6.2 完成后合并

```bash
# 1. 同步 develop 最新代码
git checkout develop
git pull --rebase origin develop

# 2. 合并功能分支（保留拓扑）
git merge --no-ff feature/user-auth

# 3. 推送
git push origin develop

# 4. 删除已合并的本地与远端分支
git branch -d feature/user-auth
git push origin --delete feature/user-auth
```

### 6.3 紧急修复

```bash
# 1. 从 main 切出
git checkout main
git pull --rebase origin main
git checkout -b hotfix/pay-callback

# 2. 修复并提交
git commit -m "fix(order): 修复支付回调重复触发导致状态错乱"

# 3. 合回 main
git checkout main
git merge --no-ff hotfix/pay-callback
git tag -a v1.0.1 -m "release: v1.0.1 修复支付回调幂等问题"
git push origin main --tags

# 4. 同步回 develop（关键，否则缺陷会再次出现）
git checkout develop
git merge --no-ff hotfix/pay-callback
git push origin develop

# 5. 删除分支
git branch -d hotfix/pay-callback
```

> **易错点**：hotfix 只合回 `main` 而未合回 `develop`，会导致下次发布时缺陷复现。这一步必须执行。

### 6.4 撤销操作

| 场景 | 命令 | 说明 |
| --- | --- | --- |
| 撤销工作区修改 | `git checkout -- <file>` | 未暂存的修改 |
| 撤销暂存 | `git restore --staged <file>` | 已 `add` 未提交 |
| 修改最后一次提交信息 | `git commit --amend` | 仅限未推送 |
| 撤销已推送的提交 | `git revert <commit>` | 生成反向提交，安全 |
| 强制回退（危险） | `git reset --hard <commit>` | 仅限本地，禁止对已推送分支使用 |

**原则**：已推送到远端的提交，只能 `revert`，不能 `reset --hard`。改写公共历史会破坏其他人的工作副本。

### 6.5 冲突处理

```bash
git pull --rebase origin develop
# 出现冲突后：
# 1. 编辑冲突文件，保留正确内容，删除冲突标记 <<<<<<< ======= >>>>>>>
# 2. 标记已解决
git add <冲突文件>
# 3. 继续 rebase
git rebase --continue
# 若想放弃本次 rebase：
git rebase --abort
```

---

## 7 仓库保护与自动化

### 7.1 分支保护规则

在 GitHub 仓库 Settings → Branches 中为 `main` 与 `develop` 配置：

| 规则 | 值 |
| --- | --- |
| Require a pull request before merging | 开启 |
| Require approvals | 1（若有多人协作） |
| Dismiss stale approvals | 开启 |
| Require status checks to pass | 开启，选择 CI 任务 |
| Require branches to be up to date | 开启 |
| Do not allow force pushes | 开启 |
| Do not allow deletions | 开启 |

### 7.2 提交信息校验

项目根目录提供 `commitlint.config.js`，配合 Husky 在 `commit-msg` 钩子校验提交信息格式。团队协作时执行：

```bash
pnpm add -D husky @commitlint/cli @commitlint/config-conventional
pnpm exec husky init
echo "npx --no -- commitlint --edit \$1" > .husky/commit-msg
```

### 7.3 持续集成

`.github/workflows/ci.yml` 定义两阶段流水线：

| 阶段 | 内容 |
| --- | --- |
| `backend` | JDK 17 + Maven 编译、单元测试 |
| `frontend` | Node 20 + pnpm 安装、构建 |

CI 在每次 push 与 PR 时自动触发，编译失败则阻断合并。

---

## 8 安全与合规

### 8.1 禁止入库的内容

| 类别 | 示例 | 处理方式 |
| --- | --- | --- |
| 数据库密码 | `password: root123` | 环境变量 `DB_PASSWORD` |
| JWT 密钥 | `secret: abcdefg` | 环境变量 `JWT_SECRET` |
| 第三方密钥 | 支付/短信 API Key | 环境变量 + `.env.example` 占位 |
| 证书私钥 | `*.pem`、`*.key`、`*.jks` | `.gitignore` 排除 |
| 本地配置 | `application-local.yml` | `.gitignore` 排除 |
| 用户数据 | 真实手机号、身份证号 | 测试数据全部虚构 |

### 8.2 密钥泄露应急处理

若不慎提交了密钥：

1. **立即轮换**：在服务端作废并重新生成密钥，这一步优先于任何 Git 操作；
2. **从历史中清除**：使用 `git filter-repo` 或 BFG Repo-Cleaner 重写历史；
3. **通知协作者**：重新克隆仓库，避免旧历史继续扩散；
4. **记录与复盘**：在 Issue 中记录事件与改进措施。

> **注意**：仅用 `git rm` 删除文件并不能从历史中移除内容，任何人仍可通过 `git log -p` 查看旧版本。

---

## 9 提交历史示例

以下是符合本规范的一段真实提交历史形态：

```
*   a1b2c3d (HEAD -> main, tag: v1.0.0) Merge branch 'develop'
|\
| *   e5f6a7b (develop) Merge branch 'feature/review-module'
| |\
| * | 9c8d7e6 feat(review): 实现评价发表与商家回复
| * | 3b2a1f0 feat(review): 新增评价规则校验
| |/
| *   b4c5d6e Merge branch 'feature/delivery-module'
| |\
| * | 7e8f9a0 feat(delivery): 实现抢单乐观锁
| * | 1a2b3c4 feat(delivery): 新增配送任务生成
| |/
| *   f0e1d2c Merge branch 'feature/order-module'
| |\
| * | 5d6e7f8 feat(order): 实现订单状态机与状态日志
| * | 8a9b0c1 feat(order): 实现订单创建与库存原子扣减
| |/
| * d2e3f4a docs: 完成设计文档 01—07
|/
* 88bb3c2 Initial commit
```

**读法**：`main` 上的合并提交对应一次发布；`develop` 上的合并提交对应一个功能完成；叶子提交是具体改动。任何人拿到仓库，几分钟就能明白项目做过什么。

---

## 10 常见问题

### Q1：提交后发现漏了文件怎么办？

```bash
git add 遗漏的文件
git commit --amend --no-edit    # 未推送时可用
```
已推送则追加一次新提交，不要 amend。

### Q2：误提交了大文件导致仓库臃肿？

```bash
# 从当前提交中移除
git rm --cached 大文件
echo "大文件" >> .gitignore
git commit -m "chore: 移除误提交的大文件"

# 若已推送且文件很大，需重写历史
git filter-repo --path 大文件 --invert-paths
```

### Q3：`git pull` 与 `git pull --rebase` 的区别？

`pull` 会产生一个合并提交，历史出现分叉；`pull --rebase` 把本地提交"搬"到远端最新提交之后，历史保持直线。本规范要求同步远端使用 `--rebase`，合并分支使用 `--no-ff`。

### Q4：如何查看某个文件的历史变更？

```bash
git log --oneline -- 文件路径
git log -p -- 文件路径
git blame 文件路径
```

### Q5：为什么提交信息要用英文 type 前缀？

工具链可解析。`feat`、`fix`、`docs` 这类前缀能被 commitlint 校验、被 `standard-version` 用于自动生成 CHANGELOG、被 CI 用于决定是否触发发布流程。描述部分用中文没有问题，前缀保持英文是为了机器可读。

---

## 11 附录

### 11.1 常用命令速查

| 目的 | 命令 |
| --- | --- |
| 查看简洁历史 | `git log --oneline --graph --all --decorate` |
| 查看某次提交详情 | `git show <commit>` |
| 查看工作区差异 | `git diff` |
| 查看暂存区差异 | `git diff --staged` |
| 暂存部分改动 | `git add -p` |
| 临时保存工作进度 | `git stash` / `git stash pop` |
| 查看分支 | `git branch -vv` |
| 删除已合并分支 | `git branch -d <branch>` |
| 查看远端分支 | `git branch -r` |
| 清理失效远端引用 | `git remote prune origin` |

### 11.2 项目内的配置文件

| 文件 | 作用 |
| --- | --- |
| `.gitignore` | 忽略规则 |
| `.gitattributes` | 换行与二进制处理 |
| `.editorconfig` | 编辑器缩进与编码统一 |
| `.gitmessage` | 提交信息模板 |
| `commitlint.config.js` | 提交信息校验规则 |
| `.github/workflows/ci.yml` | 持续集成流水线 |
| `.github/PULL_REQUEST_TEMPLATE.md` | PR 模板 |
| `.github/ISSUE_TEMPLATE/` | Issue 模板 |
| `CONTRIBUTING.md` | 贡献指南 |

### 11.3 文档修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
| --- | --- | --- | --- |
| V1.0 | 2026-10 | 初稿 | 马乾罡 |
