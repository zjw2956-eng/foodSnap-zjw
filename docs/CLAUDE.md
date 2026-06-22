# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Hope-Boot 是一款基于 Spring Boot 2.0.5.RELEASE 的多模块 Maven 脚手架项目，经过裁剪后用于开发美食分享社交平台。技术栈：Spring Boot + Apache Shiro + MyBatis/tk.MyBatis + Redis + Flyway + Thymeleaf + Swagger + Minio（图片存储）+ Redisson（分布式锁）+ RocketMQ（异步消息）。

项目目的：用户拍照上传美食照片，AI 辅助识别菜品并生成精致文案，浏览者以弹幕形式互动，点赞、评论、收藏。根据综合评分形成每日区域美食榜单，解决"今天吃什么"的选择困难。

双榜机制：实时热榜（每 30 分钟刷新，近 6 小时高赞帖子）+ 每日金榜（0 点 Quartz 结算锁定）。AI 部分由独立 Python 服务处理，Java 主服务通过 HTTP REST 调用。

已删除原项目的 RBAC 权限模型（角色-资源五表），权限控制简化为 SysUser 的 `role` 字段（admin/user）。

JDK 版本：1.8。编码：UTF-8。项目使用 GPL-v3.0 协议。

## 构建与运行命令

```bash
# 整体打包（在根目录执行）
mvn clean install

# 只编译不运行测试
mvn clean install -DskipTests

# 运行单个模块（在对应模块目录下执行）
mvn spring-boot:run
```

## 模块架构

项目包含 7 个子模块，每个都是独立可运行的 Spring Boot 应用：

| 模块 | 用途 | 入口类 | 端口 |
|------|------|--------|------|
| `hope-admin` | 后台管理系统主模块 | `HopeAdminApplication` | 8886 |
| `hope-core` | 核心业务逻辑（Mapper/Service/Shiro 配置） | 不可独立运行 | — |
| `hope-framework` | 框架基础设施（通用基类、配置、工具） | 不可独立运行 | — |
| `hope-flyway` | 数据库版本迁移（Flyway，最早运行） | `HopeFlywayApplication` | 默认 |
| `hope-sso-server` | 单点登录认证中心（基于 xxl-sso） | `HopeSsoServerApplication` | 8887 |
| `hope-generator` | 代码生成器（基于 xxl-code-generator） | `HopeGeneratorApplication` | 8888 |
| `hope-quartz` | 定时任务（每日金榜结算、热榜刷新） | `HopeQuartzApplication` | 8889 |

**模块依赖关系**：`hope-admin` → `hope-core` → `hope-framework`。`hope-sso-server`、`hope-generator`、`hope-quartz`、`hope-flyway` 是独立模块。

## 方案文档

- `docs/实施计划.md` — 完整项目链路、功能模块、技术栈、实施阶段
- `docs/sql/food_snap_schema.sql` — 建表语句（7 张新表 + sys_user 补充字段）
- `docs/架构升级计划.md` — 架构演进规划（已完成的 Redis 化、已完成的 RocketMQ 异步化、待做的缓存分层/审核漏斗等）
- `docs/RocketMQ异步发帖方案.md` — RocketMQ 异步发帖方案设计（事件驱动、Topic/Group、重试梯度、可靠性）
- `docs/RocketMq部署(Windows docker desktop).md` — Windows Docker Desktop 部署 RocketMQ 5.4.0 教程
- `docs/技术栈说明.md` — 各技术栈的科普向说明（Flyway/Shiro/JPA/SSO/RBAC/MyBatis/Python/Quartz）

## 启动顺序

1. 先创建数据库 `foodSnap`，字符集 `utf8mb4`
2. 配置各模块的 `application.yml` 中的数据库和 Redis 连接信息
3. 先运行 `hope-flyway`（`HopeFlywayApplication`）完成数据库表结构初始化
4. 再运行 `hope-admin`（`HopeAdminApplication`）启动后台管理
5. 按需运行 `hope-sso-server`、`hope-generator`、`hope-quartz`

后台登录账号：`admin`，密码：`123456`。Swagger API 文档地址：`http://localhost:8886/swagger-ui.html`。

## 核心架构要点

### 安全认证（hope-core/shiro/）

Shiro 配置类为 `ShiroConfig`（`@Order(-1)`），核心组件：
- **Realm**: `HopeShiroRealm` — 自定义认证/授权逻辑
- **凭证匹配器**: `RetryLimitCredentialsMatcher` — 带重试次数限制的密码匹配（MD5 哈希，2 次迭代）
- **缓存**: Redis 实现（`shiro-redis` 插件），管理 session 和权限缓存
- **Session 管理**: `DefaultWebSessionManager` + `RedisSessionDAO`，session 持久化到 Redis
- **并发控制**: `KickoutSessionControlFilter` — 同一账号最多 5 个并发 session
- **记住我**: Cookie-based，AES 加密密钥为 `1QWLxg+NYmxraMoxAXu/Iw==`
- Shiro 过滤器链采用硬编码配置（`ShiroConfig.shiroFilter()`），不再从数据库加载

### 通用 CRUD 基类（hope-framework/）

- `BaseService<T>` / `BaseServiceImpl<T>` — 基于 tk.MyBatis 通用 Mapper 的封装，提供 `insert`、`deleteById`、`selectById`、`updateById`、`listAll` 等方法
- `AbstractCrudService` — 基于 Spring Data JPA 的 CRUD 封装
- `ResponseVo<T>` — 统一响应体，使用 Fastjson 序列化（`status`、`message`、`data`）

### 数据层

使用 MyBatis + tk.MyBatis 通用 Mapper + PageHelper 分页插件。`hope-core` 中的 Mapper 接口放在 `com.hope.mapper` 包下，继承 `BaseMapper<T>` 即可获得基本 CRUD 能力。

### 用户与权限

`SysUser` 表新增 `role` 字段（`admin` / `user`），区分管理员和普通用户。`HopeShiroRealm.doGetAuthorizationInfo()` 直接读取该字段完成授权，不再查询角色/资源关联表。

### 配置类（hope-framework/config/）

- `DruidConfig` — 阿里 Druid 连接池 + 监控
- `RedisConfig` — Redis 配置
- `SwaggerConfig` — API 文档
- `KaptchaConfig` — Google 验证码
- `MvcConfig` — Spring MVC 配置

### 数据库迁移

hope-flyway 模块使用 Flyway 管理数据库版本。SQL 迁移脚本位于 `hope-flyway/src/main/resources/db/migration/` 目录下，遵循 Flyway 命名规范 `V{version}__{description}.sql`。

## 注意事项

- 运行前需要本地安装 Redis 服务
- 根 POM 的 `<packaging>` 是 `pom`，所有公共依赖（spring-boot-starter-web、lombok、hutool）在根 POM `<dependencies>` 中统一引入，子模块自动继承
- hope-flyway 模块没有配置打包插件（不用于部署）
- 前端页面复用自 RuoYi 项目
- 新增依赖 Minio（图片对象存储）和 Redisson（分布式锁），运行前需本地安装 Minio 服务
- AI 分析由独立 Python 服务（FastAPI）提供，部署时需单独启动
- RocketMQ 已接入，承担发帖异步化和后续 AI 分析/内容审核的事件驱动链路。详见 `docs/RocketMQ异步发帖方案.md` 和 `docs/RocketMq部署(Windows docker desktop).md`
