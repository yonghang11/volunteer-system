# 学生实训室选组选拔系统 —— 期末作业交付说明

> 技术栈：Vue 3 + Vite 5 + Element Plus + Pinia ｜ Spring Boot 3.2.5 + MyBatis-Plus 3.5.6 + MySQL ｜ JWT 鉴权
> 部署形态：阿里云轻量应用服务器（Ubuntu 24.04 / 1C1G）+ 宝塔面板 + Nginx 反向代理
> 文档版本：v1.0　生成时间：2026-11-20

---

## 0. 系统概述

系统面向"学生实训室选组"这一真实业务，把选拔过程拆成 **8 个受时间窗驱动的环节**，四种角色在同一套系统里各司其职：

| 环节 | 编码 | 参与角色 | 核心动作 |
|---|---|---|---|
| ① 集中宣讲 | BRIEFING | 学生 / 学院管理员 | 查看宣讲排期与内容 |
| ② 实训室开放日 | OPEN_DAY | 学生 / 实训室管理员 | 预约参观、了解方向 |
| ③ 意向填报 | INTENT | 学生 | 填报最多 3 个**平行**意向 |
| ④ 面试考核 | INTERVIEW | 导师 / 学生 | 排期、评分（4 维度） |
| ⑤ 拟录取确认 | OFFER | 导师 / 学生 | 发 offer、学生确认 |
| ⑥ 分轮补录 | SUPPLEMENT | 实训室管理员 | 第 2、3 轮补录 |
| ⑦ 统筹调剂 | TRANSFER | 学院管理员 | 内部调剂 → 跨室调剂 → 约谈安置 |
| ⑧ 公示入组 | PUBLIC | 学院管理员 / 学生 | 公示、异议、正式入组 |

四类角色：`COLLEGE_ADMIN`（学院管理员）、`LAB_ADMIN`（实训室管理员）、`MENTOR`（导师）、`STUDENT`（学生）。

---

## 一、UI / UX 设计

### 现状（已完成）

- **按角色分流的信息架构**：登录后依据 `role` 进入独立工作台，学生、导师、实训室管理员、学院管理员互不干扰。
- **页面清单**（8 个路由级页面）：
  | 页面 | 文件 | 设计要点 |
  |---|---|---|
  | 登录 | `views/Login.vue` | 快捷演示账号一键填充，降低演示门槛 |
  | 学生首页 | `views/student/Home.vue` | 当前环节倒计时 + 待办引导 |
  | 意向填报 | `views/student/Intent.vue` | 3 个平行志愿、服从调剂开关、简历上传 |
  | 面试信息 | `views/student/Interview.vue` | 场次、地点、状态可视 |
  | 结果查询 | `views/student/Result.vue` | offer 确认 / 公示 / 入组结果 |
  | 导师评审 | `views/mentor/Review.vue` | 意向列表 → 排面试 → 4 维评分 |
  | 实训室管理 | `views/lab/Manage.vue` | 名额、补录、内部调剂 |
  | 学院管理 | `views/admin/Admin.vue` | 全局进度、时间窗配置、统筹调剂 |
- **视觉体系**：Element Plus 组件按需引入 + CSS 变量统一主色；Vite `manualChunks` 把 Vue 生态单独拆包，首屏体积可控。
- **交互反馈**：统一的 `ElMessage` 提示、按钮 loading 态、表单校验提示。

### 可补强的加分项（建议按需选做）

1. **空态与骨架屏**：意向未开放、无可面试学生时给出明确引导文案而非空白表格。
2. **环节状态可视化**：用 `el-steps` 把 8 环节做成进度条，学生一眼看懂"我在第几步"。
3. **响应式**：当前以桌面端为主，可补充 768px 断点下的卡片化布局。
4. **无障碍与可读性**：表单 `label` 关联、对比度检查、键盘可达。
5. **设计规范文档**：色板、字号、间距、圆角四项定稿，避免各页面各自为政。

---

## 二、后端逻辑实现

### 现状（已完成）

- **分层结构**：`controller → service → mapper → entity`，DTO 与实体分离，`common` 下统一 `Result`、`BizException`、`GlobalExceptionHandler`、`Const` 常量枚举。
- **13 个控制器**：Auth、Catalog、Intent、Bio、Interview、Offer、Supplement、Transfer、Public、Stage、Lab、Mentor、Admin，与 8 个业务环节一一对应。
- **鉴权**：无状态 JWT，`Authorization: Bearer <token>`；前端 axios 拦截器统一注入，401 自动跳登录。
- **统一异常**：`GlobalExceptionHandler` 兜底，业务异常返回统一 `code/message`，前端按 `code === 0` 判成功。
- **参数校验**：`spring-boot-starter-validation` + `@Valid`，边界输入在入口拦截。
- **导出能力**：EasyExcel 支持公示名单 / 结果导出。

### 必须守住的业务规则（已在代码中体现）

| 规则 | 落点 |
|---|---|
| 意向最多 3 个且平行（不分先后） | `intent` 唯一键 `(student_id, mentor_id, round_no)` + 服务层数量校验 |
| 一个学生同一轮仅一场面试 | `interview` 唯一键 `(student_id, round_no)` |
| 一场面试仅一份评分 | `score.interview_id` UNIQUE |
| 同一学生同一轮同一类型仅一条 offer | `offer` 唯一键 `(student_id, mentor_id, round_no, type)` |
| 正式入组唯一 | `join_record` 唯一键 `(student_id)` |
| 环节开关由时间窗驱动 | `stage_window` 表 + `StageController`，前端不硬编码日期 |

### 建议补强（答辩可讲的技术点）

1. **名额扣减并发安全**：录取确认时对 `mentor.quota` 做 `UPDATE ... WHERE quota > used` 条件更新，或加版本号乐观锁，避免超发。
2. **状态机集中化**：把 `SUBMITTED → OFFERED → CONFIRMED` 等流转收敛到一个 `IntentStateMachine`，禁止非法跳转。
3. **接口幂等**：确认 offer、提交意向加唯一约束 + 重复提交友好提示。
4. **操作留痕**：`audit_log` 目前有表，建议在关键动作（调剂、公示、补录）统一写入。

---

## 三、数据库设计

### 表结构（13 张）

| 表 | 说明 | 关键约束 |
|---|---|---|
| `sys_user` | 四类角色统一用户表 | `username` UNIQUE；`role` 区分身份 |
| `lab` | 实训室（4 个） | `code` UNIQUE |
| `mentor` | 导师，意向填报单位 | `idx_mentor_lab` |
| `stage_window` | 8 环节时间窗，驱动全站开关 | `stage_code` UNIQUE |
| `session_info` | 宣讲 / 开放日排期 | `type` = BRIEFING / OPEN_DAY |
| `intent` | 学生平行意向 | `uk_stu_mentor_round` |
| `interview` | 面试场次 | `uk_interview(student_id, round_no)` |
| `score` | 4 维评分（态度/逻辑/编程/投入） | `interview_id` UNIQUE |
| `offer` | 拟录取 / 递补 / 内部调剂候选 | `uk_offer` + 两个查询索引 |
| `supplement_round` | 补录轮次与名额快照 | `round_no` UNIQUE |
| `transfer_task` | 统筹调剂（1 内部 / 2 跨室 / 3 约谈） | `idx_transfer_stu` |
| `public_list` | 公示名单与异议 | `idx_public_stu` |
| `join_record` | 正式入组 | `uk_join_stu` |
| `notice` / `audit_log` | 站内通知 / 操作留痕 | 按用户与时间建索引 |

### 设计要点

- **字符集**：`utf8mb4 / utf8mb4_general_ci`，支持中文与 emoji。
- **唯一键 = 业务规则**：把"不能重复填同一导师""一人只能入组一次"这类规则下沉到数据库，代码层兜底。
- **索引覆盖高频查询**：`intent(lab_id, round_no)`、`offer(student_id, status)`、`offer(mentor_id, type, status)`。
- **可重复构建**：`db/schema.sql` 建库建表，`db/seed.sql` 灌入 49 用户 / 4 实训室 / 12 导师 / 8 时间窗。

### ⚠️ 已修正的不一致

原脚本建库名为 `volunteer_system`，而后端 `application.yml` 连接的库是 `zytbxt`，两者对不上会导致"脚本执行成功但系统连不上数据"。

已在本次交付中统一为 **`zytbxt`**（`schema.sql`、`seed.sql` 均已修改），与服务器现有库一致。

---

## 四、代码管理规范化

### 现状

- 已初始化 Git 仓库，共 2 次提交：
  ```
  db52c72 chore: 移除体积过大的node.exe本地环境文件
  4d5eb26 初始化项目
  ```
- `.gitignore` 已覆盖 `node_modules/`、`dist/`、`target/`、`*.log`、IDE 目录、`.tools/`。

### 规范建议（可直接照做）

1. **提交信息采用 Conventional Commits**
   ```
   feat(intent): 支持填报 3 个平行意向
   fix(deploy): 修正 application.yml 缩进导致的启动失败
   docs(readme): 补充宝塔部署步骤
   chore(git): 忽略本地工具链目录
   ```
2. **分支策略（Git Flow 简化版）**：`main`（可部署）← `develop`（集成分支）← `feature/*`、`fix/*`。
3. **禁止入库的内容**：构建产物（`dist/`、`target/`）、依赖目录、日志、**数据库密码**、`.tools/` 工具链。
4. **建议补充 `README.md`**：一键启动命令、默认账号、目录结构、部署入口。
5. **建议补充 `.env.example`**：列出 `DB_URL / DB_USERNAME / DB_PASSWORD / TOKEN_SECRET / UPLOAD_DIR`，真实值走环境变量。
6. **版本标记**：交付前打 tag，例如 `v1.0.0-deploy`，与 jar 版本号对应。

### 当前未提交的变更（交付时应提交）

```bash
git add volunteer-system/backend/src/main/resources/application.yml \
        volunteer-system/db/schema.sql \
        volunteer-system/db/seed.sql \
        volunteer-system/frontend/src/api/http.js \
        volunteer-system/frontend/.env.production
git commit -m "fix(deploy): 修复 YAML 启动错误并统一数据库名与环境化接口地址"
```

> 注：`.tools/inspect_jar.ps1` 为本地排查脚本，不应入库。

---

## 五、团队协作

### 角色分工（按系统权限设计，可直接映射到组员）

| 角色 | 职责 | 对应模块 |
|---|---|---|
| 项目负责人 / 架构 | 数据库设计、接口契约、进度把控 | `db/`、`common/`、接口文档 |
| 前端 A | 学生端全流程 | Login、Home、Intent、Interview、Result |
| 前端 B | 管理与评审端 | admin、lab、mentor 页面 |
| 后端 A | 意向 / 面试 / 评分链路 | Intent、Interview、Offer |
| 后端 B | 补录 / 调剂 / 公示 / 导出 | Supplement、Transfer、Public、Admin |
| 测试 + 运维 | 用例、部署、演示脚本 | 部署文档、验收清单 |

### 协作机制

1. **接口先行**：先用一份接口清单（路径 / 方法 / 入参 / 出参 / 错误码）对齐前后端，再并行开发。
2. **PR 评审**：每次合并到 `develop` 至少 1 人 review，重点检查权限校验与 SQL 条件。
3. **Issue 驱动**：功能拆成 Issue，用 `feat:` / `fix:` 提交关联编号。
4. **每日 15 分钟同步**：昨天做了什么、今天做什么、有什么阻塞。
5. **演示脚本**：固定一条"学生填报 → 导师评分 → 管理员公示"的演示路径，避免现场踩空。

---

## 六、部署上线

### 6.1 服务器拓扑

```
公网 :80  →  Nginx（宝塔站点）
               ├── /          → /www/wwwroot/frontend  （Vue dist，SPA 伪静态）
               └── /api/      → http://127.0.0.1:8080  （Spring Boot）
                                     └── MySQL 127.0.0.1:3306 / 库 zytbxt
```

### 6.2 本次交付产物（已验证）

| 项 | 值 |
|---|---|
| jar 路径 | `backend/target/volunteer-1.0.0.jar` |
| 大小 | 52,487,479 字节 |
| MD5 | `6EFE7E4D7B6DBD2C4405540794D9C661` |
| 验证 | 本地 `java -jar` 启动成功，`/api/auth/login` 返回 200 |
| 说明 | 修复了 `application.yml` 缩进错误；数据库连接改为环境变量可覆盖 |

**服务器上旧包（MD5 `F94C566D420D8E76088AAA31234F0AB7`）必须替换**，它内含缩进错误的 YAML，启动即崩。

### 6.3 后端启动参数（1C1G 服务器）

```bash
cd /www/wwwroot/backend
md5sum volunteer-1.0.0.jar    # 必须为 6EFE7E4D7B6DBD2C4405540794D9C661

ps -ef | grep volunteer | grep -v grep | awk '{print $2}' | xargs -r kill

nohup java -Xms128m -Xmx384m -Xss512k -XX:MaxMetaspaceSize=192m \
  -XX:+UseSerialGC -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 \
  -jar volunteer-1.0.0.jar > app.log 2>&1 &
```

内存预算：堆 384M + 元空间 192M ≈ 600M，余量留给 Nginx 与 MySQL 5.7。建议额外配置 2G Swap 兜底。

可用环境变量覆盖配置，无需重新打包：

```bash
DB_URL=jdbc:mysql://127.0.0.1:3306/zytbxt?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false
DB_USERNAME=zytbxt
DB_PASSWORD=实际密码
TOKEN_SECRET=生产环境随机串
UPLOAD_DIR=/www/wwwroot/backend/uploads
```

### 6.4 Nginx 站点配置

```nginx
server {
    listen 80;
    server_name 47.93.228.116;

    root /www/wwwroot/frontend;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_read_timeout 60s;
        client_max_body_size 20m;
    }
}
```

`proxy_pass` 结尾**不能加 `/`**，否则 `/api` 前缀会被吃掉，后端 404。

### 6.5 前端接口地址策略

```js
// src/api/http.js
baseURL: import.meta.env.VITE_API_BASE_URL || '/api'
```

- 本地开发：无该变量 → `/api` → Vite 代理转发到 `localhost:8080`
- 生产构建：读 `.env.production` → `http://47.93.228.116/api`

> 若改为完全走 Nginx 反代（更推荐，避免 IP 写死），把 `.env.production` 的值改成 `/api` 后重新 `npm run build` 即可。

### 6.6 上线验收清单

- [ ] `md5sum volunteer-1.0.0.jar` 与交付值一致
- [ ] `curl -X POST http://127.0.0.1:8080/api/auth/login` 返回 200
- [ ] 浏览器访问站点首页正常渲染（非 502）
- [ ] 登录 `stu01 / 123456` 成功，能进入学生工作台
- [ ] 刷新任意子路由（如 `/student/intent`）不出现 404（伪静态生效）
- [ ] 简历上传成功（验证 `client_max_body_size` 与 `upload-dir` 权限）
- [ ] 时间窗修改后全站开关随之变化
- [ ] 1G 内存下连续操作 10 分钟，`free -m` 无持续下降

### 6.7 遗留问题（与代码无关）

宝塔面板 8888 端口当前不可访问（实测 22 / 80 通，8888 不通），需 SSH 登录后确认：

```bash
cat /www/server/panel/data/port.pl   # 面板真实端口，可能已非 8888
bt status && bt restart
```

同时检查云服务器安全组是否放行该端口。

---

## 七、交付物清单

| 类别 | 交付物 | 位置 |
|---|---|---|
| 源码 | 前端 Vue 3 项目 | `volunteer-system/frontend` |
| 源码 | 后端 Spring Boot 项目 | `volunteer-system/backend` |
| 数据库 | 建表 + 种子脚本 | `volunteer-system/db/{schema,seed}.sql` |
| 构建产物 | 可运行 jar（已修复） | `backend/target/volunteer-1.0.0.jar` |
| 构建产物 | 前端静态包 | `frontend/dist` |
| 本文档 | 六项要求交付说明 | 根目录 |

---

## 八、后续优先级建议

| 优先级 | 事项 | 理由 |
|---|---|---|
| P0 | 替换服务器上的旧 jar 并验证登录 | 直接决定系统能否上线 |
| P0 | 确认服务器 MySQL 密码与库名 | 启动失败的第二大原因 |
| P1 | 提交本次变更并打 tag | 代码管理项得分点 |
| P1 | 补齐 README 与 `.env.example` | 交付规范性，评审常看 |
| P2 | 名额并发安全、状态机集中化 | 后端逻辑深度加分 |
| P2 | 环节进度条、空态优化 | UI/UX 得分点 |
