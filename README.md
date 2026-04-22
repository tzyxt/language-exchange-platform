# 基于智能匹配的中外学生语言互助与跨文化交流平台

本项目根据 `docs/功能文档.md`、`docs/设计文档.md`、`docs/数据库表结构明细.md` 落地为一个可演示的全栈 MVP，覆盖注册登录、资料完善、智能匹配、聊天、社区、活动、我的中心和后台管理等核心模块。

## 技术栈

- 前端：`Vue 3 + Vite + Vue Router`
- 后端：`Spring Boot 3`
- 数据库：`MySQL` 建表脚本位于 [database/schema.sql](/e:/基于智能匹配的中外学生语言互助与跨文化交流平台设计与实现/database/schema.sql)
- 当前演示模式：后端采用内置内存数据，便于直接联调和答辩演示；后续可平滑切换到 MySQL 持久化

## 项目结构

- [backend](/e:/基于智能匹配的中外学生语言互助与跨文化交流平台设计与实现/backend)
  Spring Boot 接口服务，已实现用户端与管理端核心接口
- [frontend](/e:/基于智能匹配的中外学生语言互助与跨文化交流平台设计与实现/frontend)
  Vue 3 单页应用，包含 16 个主要页面
- [docs](/e:/基于智能匹配的中外学生语言互助与跨文化交流平台设计与实现/docs)
  需求、设计与表结构文档
- [database](/e:/基于智能匹配的中外学生语言互助与跨文化交流平台设计与实现/database)
  MySQL 建库建表脚本

## 已实现模块

- 用户认证：登录、注册、找回密码
- 用户画像：资料编辑、语言设置、兴趣偏好、个人简介
- 智能匹配：候选推荐、条件筛选、联系发起、反馈记录
- 聊天互动：会话列表、消息列表、文本发送
- 社区分享：发帖、点赞、收藏、分享、评论、搜索
- 活动管理：活动列表、活动详情、报名、取消报名、活动评价
- 我的中心：匹配统计、活动统计、通知列表
- 后台管理：统计总览、用户管理、活动管理、举报处理

## 前端页面

- 用户端：登录、注册、找回密码、首页、个人资料、匹配推荐、用户详情、聊天会话、社区分享、活动列表、活动详情、我的中心
- 管理端：后台首页、用户管理、活动管理、举报与内容审核

## 运行方式

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认端口：`8080`

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认端口：`5173`

### 3. 一键启动

在项目根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\start-all.ps1
```

或分别执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\start-backend.ps1
powershell -ExecutionPolicy Bypass -File .\start-frontend.ps1
```

## 演示账号

- 普通用户：`lily.chen / 123456`
- 管理员：`admin / 123456`

## 说明

- 当前后端为 `demo-mode`，数据由 `DataStore` 内置初始化，适合课程设计与毕业答辩演示。
- `database/schema.sql` 已保留正式 MySQL 表结构，可在下一阶段继续接入 `JPA/MyBatis` 完成真实持久化。
- 如果本地 Maven 或 npm 首次拉取依赖失败，需要确保依赖仓库具备读写权限和网络访问能力。
- 在当前环境中，`mvn spring-boot:run` 可能因 Windows 中文路径下的 Spring Boot Maven 插件启动兼容问题失败；使用 `start-backend.ps1` 或 `mvn package` 后 `java -jar target/language-exchange-platform-0.0.1-SNAPSHOT.jar` 可以正常运行。
