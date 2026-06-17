# RocketMQ 异步发帖削峰方案

## 一、目标

将发帖流程从同步改为异步，核心发布路径在 3 秒内返回，AI 分析、内容审核、图片处理等耗时操作全部通过 RocketMQ 异步消费。

## 二、核心思路：事件驱动

### 不是"发指令"，而是"发事实"

```
❌ 消息思维：Producer 发"请做AI分析"，必须知道 Consumer 的存在
✅ 事件思维：Producer 广播"帖子已创建"，不管谁消费
```

一个事件，多个 Consumer 独立订阅，互不感知。加新功能 = 加新 Consumer 文件，不动 Producer。

## 三、发帖流程对比

### 升级前（同步）

```
用户发帖 → 校验 → Minio上传 → DB写入 → 返回成功
                                      ↑
                                  等待 AI 分析（30s）
                                  用户一直等...
```

### 升级后（异步事件驱动）

```
┌─ 同步主链路（用户感知，<3秒）───────┐
│                                     │
│  用户发帖                            │
│    → 基本校验（文件大小、格式）       │
│    → Minio 上传图片                  │
│    → DB 写入（status=2，AI分析中）    │
│    → 发送事件"PostCreated"           │
│    → 返回"发布成功，AI分析中"          │
│                                     │
└─────────────────────────────────────┘
              ↓ 事件解耦
┌─ 异步处理链（后台，用户无感）────────┐
│                                     │
│  Consumer 1：AI 分析                │
│    → 调 Python AI 服务              │
│    → 识别菜品 + 生成文案 + 同款建议  │
│    → 更新 food_post（foodName、     │
│       description）                 │
│    → status 改为 1（正常）           │
│                                     │
│  Consumer 2：内容审核（预留）         │
│    → 图片鉴黄鉴暴                    │
│    → 非食物图片识别                  │
│    → 违规自动下架                    │
│                                     │
│  Consumer 3：图片处理（预留）         │
│    → 压缩生成缩略图                  │
│    → WebP 格式转换                  │
│                                     │
│  Consumer 4：搜索索引（预留）         │
│    → 写入 Elasticsearch             │
│    → 用户可搜索帖子                  │
│                                     │
└─────────────────────────────────────┘
```

## 四、代码结构

```
com.hope.mq
├── config/
│   └── RocketMQConfig.java          -- 连接 NameServer，创建 Producer/Consumer
├── event/
│   └── PostCreatedEvent.java        -- 事件体：postId, userId, imageUrl, timestamp
├── producer/
│   └── PostEventProducer.java       -- 发事件，Controller 调用
└── consumer/
    ├── AiAnalysisConsumer.java      -- AI 分析（当前实现）
    ├── ContentAuditConsumer.java    -- 内容审核（预留，TODO）
    └── PostStatsConsumer.java       -- 用户统计（预留，TODO）
```

**原则**：一个 Consumer 一个文件，只做一件事。

## 五、事件体设计

`PostCreatedEvent`：只带最小必要信息

| 字段 | 类型 | 说明 |
|------|------|------|
| postId | Integer | 帖子 ID，Consumer 根据此自己查 DB |
| postUuid | String | 帖子 UUID，前端识别用 |
| userId | Integer | 发帖用户 |
| imageUrl | String | 图片在 Minio 的地址 |
| timestamp | Long | 事件发生时间 |

**为什么不塞整个 FoodPost？** 事件是快照，Consumer 处理时数据可能已变化（如用户修改了文案）。Consumer 拿到 postId 后自己 SELECT 最新数据。

## 六、Topic 和 Consumer Group 设计

| Topic | Consumer Group | 用途 | 状态 |
|-------|---------------|------|------|
| `post-created` | `ai-analysis-group` | AI 菜品识别 + 文案生成 | ✅ 实现 |
| `post-created` | `content-audit-group` | 图片审核 | 📅 预留 |
| `post-created` | `post-stats-group` | 更新用户发帖数 | 📅 预留 |

多个 Consumer Group 订阅同一个 Topic，每个 Group 都能收到消息，各自独立消费。

## 七、Consumer 消费流程

```
1. 应用启动 → Consumer Bean 创建 → @PostConstruct 启动监听
2. 循环拉取消息 → 收到 PostCreatedEvent
3. 根据 postId 查询 FoodPost（最新数据）
4. 执行业务逻辑（调 AI / 审核 / 压缩）
5. 更新 DB
6. 返回 CONSUME_SUCCESS（RocketMQ 标记消费完成）
```

**异常处理**：
- 调 AI 超时 → 记录日志，post 保持 status=2，下次定时任务重试
- 调 AI 失败 → 重试 3 次 → 仍失败写入 food_ai_analysis 表（status=3，记录错误原因）
- Consumer 挂了 → RocketMQ 自动重投递给其他节点

## 八、面试叙事

> "发帖是社交平台最核心的链路。我设计了事件驱动架构来应对高并发发帖场景——核心路径只做校验、上传和写库，3 秒内返回。AI 识别、内容审核、图片处理全部通过 RocketMQ 异步消费。"
>
> "关键设计是'事件而非消息'——Producer 只广播'帖子已创建'这个事实，不关心谁消费。这样架构是松耦合的，后续加审核 Consumer、搜索索引 Consumer，只需新建文件订阅同一个 Topic，完全不碰现有代码。"
>
> "选择 RocketMQ 而不是 Kafka，因为发帖场景需要延迟消息（给用户修改文案的时间窗口）和事务消息（保证 DB 写入和消息发送的原子性），这两点 RocketMQ 都是开箱即用的。"

## 九、依赖与配置

### Maven 依赖

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-client</artifactId>
    <version>5.3.1</version>
</dependency>
```

> 注意：5.x 版本兼容 4.x 的传统 API（DefaultMQProducer/DefaultMQPushConsumer），也提供新的 gRPC 代理模式。当前方案使用传统 API，无需额外部署 Proxy 组件。

### 配置项

```yaml
rocketmq:
  name-server: localhost:9876
  producer:
    group: food-snap-producer
  consumer:
    ai-analysis-group: ai-analysis-consumer
```

### 本地运行 RocketMQ

**Docker Desktop（推荐）**：

```bash
# NameServer
docker run -d --name rmq-namesrv -p 9876:9876 apache/rocketmq:5.3.1 sh mqnamesrv

# Broker
docker run -d --name rmq-broker \
  -p 10911:10911 -p 10909:10909 \
  -e "NAMESRV_ADDR=rmq-namesrv:9876" \
  apache/rocketmq:5.3.1 sh mqbroker -c /home/rocketmq/conf/broker.conf
```

或使用 `docker-compose.yml` 一键启动（推荐，方便管理）：

```yaml
version: '3'
services:
  namesrv:
    image: apache/rocketmq:5.3.1
    container_name: rmq-namesrv
    ports:
      - "9876:9876"
    command: sh mqnamesrv

  broker:
    image: apache/rocketmq:5.3.1
    container_name: rmq-broker
    ports:
      - "10911:10911"
      - "10909:10909"
    environment:
      - NAMESRV_ADDR=namesrv:9876
    command: sh mqbroker -c /home/rocketmq/conf/broker.conf
    depends_on:
      - namesrv
```

> 本地开发单节点足够，生产环境通过配置文件切换为 2m-2s 集群（DLedger Raft 多副本）。

## 十、Controller 改动点

`FoodPostController.upload()` 只改两行：

1. `foodPost.setStatus(2);` — 初始状态改为"AI 分析中"
2. `postEventProducer.send(postId);` — DB 写入后、return 之前，发事件

其他不动。

---

## 十一、消息确认机制（ACK）

### 消费端确认

Consumer 处理完消息，必须返回状态：

| 返回状态 | 含义 | Broker 行为 |
|---------|------|-------------|
| `CONSUME_SUCCESS` | 消费成功 | 更新消费进度，不再投递 |
| `RECONSUME_LATER` | 消费失败，需重试 | 进入重试队列，按延迟梯度重新投递 |

### 重试梯度

```
第 1 次失败 → 10s 后重试
第 2 次失败 → 30s 后重试
第 3 次失败 → 1min 后重试
...
第 16 次失败 → 进入死信队列（DLQ，Dead Letter Queue）
```

> 死信队列是兜底——16 次都失败说明不是临时故障，需人工介入排查。

### Consumer 消费逻辑

```
收到消息
  → try {
      调 AI 分析
      更新 DB
      return CONSUME_SUCCESS         ← 成功
    } catch (AI超时) {
      return RECONSUME_LATER         ← 临时故障，重试
    } catch (AI不可恢复错误) {
      写 food_ai_analysis（status=3，记录错误原因）
      return CONSUME_SUCCESS         ← 业务失败但消费成功，不重试
    }
```

**关键区分**：临时性故障（网络超时、服务不可用）→ 重试；永久性故障（图片无法识别、格式不支持）→ 记录失败 → 消费成功。

---

## 十二、生产者发送方式

| 方式 | 含义 | 可靠性 | 性能 | foodSnap 场景 |
|------|------|--------|------|-------------|
| **同步发送** | 发完等 Broker 确认再返回 | 最高 | 中 | ✅ 发帖事件——不能丢 |
| 异步发送 | 发完立即返回，回调收确认 | 中 | 高 | 批量操作 |
| 单向发送 | 发完不管 | 低 | 最高 | 埋点、日志 |

**发帖用同步发送**：宁可多等 50ms，事件不能丢。

---

## 十三、消息可靠性

### 发帖 + 发事件的原子性问题

```
发帖流程：
  ✓ food_post 写入成功
  ✗ 发 RocketMQ 事件失败  → 帖子写了但 AI 不知道
```

### 当前方案：补偿任务（实现简单，可靠性高）

```
定时任务（每 5 分钟）：
  SELECT * FROM food_post
  WHERE status = 2
    AND create_time < NOW() - 5min
  → 补发 PostCreatedEvent
```

扫描"5 分钟了还停留在 AI 分析中"的帖子，补发事件。

### 后续升级方案：事务消息（半消息）

```
1. 发半消息 → Broker 暂存，不投递
2. 写 food_post → 本地事务
3. 成功 → commit → Consumer 可见
   失败 → rollback → 消息废弃
4. Producer 挂掉没回执 → Broker 回查本地事务状态
```

> 当前用补偿任务保证最终一致性，事务消息在后续版本迭代中引入。面试时可以主动提这个演进思路。

---

## 十四、高可用机制

### Broker 端

| 机制 | 说明 |
|------|------|
| **DLedger（5.x 内置）** | 基于 Raft 多副本一致性协议，自动选主、故障转移 |
| **同步刷盘** | 消息落盘才返回成功，可靠性最高，性能最低 |
| **异步刷盘** | 写入 PageCache 返回，性能高，宕机可能丢少量消息 |
| **主从架构** | Master 负责写，Slave 负责读和备份 |

### Consumer 端

| 机制 | 说明 | 你的配置 |
|------|------|---------|
| **集群消费** | 同 Group 内一条消息只被一个节点消费 | ✅ 使用此模式 |
| **广播消费** | 所有节点都收到所有消息 | ❌ 不适用 |
| **故障转移** | Consumer 挂掉，剩余节点自动接管队列 | ✅ 自动支持 |
| **负载均衡** | 队列数 ≥ Consumer 数时自动均匀分配 | ✅ 自动支持 |

### 消费模式选择

```
集群消费（推荐）：
  Consumer A、B 同属 ai-analysis-group
  post-1 → Consumer A 处理（仅一次）
  post-2 → Consumer B 处理（仅一次）

广播消费（不适用）：
  Consumer A、B 都收到 post-1、post-2
  post-1 被分析两次！
```

**你用集群消费**——一个帖子只被分析一次，多 Consumer 节点分担负载。

---

## 十五、本地开发 vs 生产环境

| 维度 | 本地开发 | 生产环境 |
|------|---------|---------|
| NameServer | 1 个 Docker 容器 | 2 个节点 |
| Broker | 1 个 Docker 容器（单 Master） | 2m-2s（DLedger Raft） |
| 刷盘 | 异步刷盘 | 同步刷盘 |
| 事务消息 | 补偿任务 | 事务消息 + 补偿双保险 |
| 应用 | hope-admin 8086 | 多实例 + Nginx |
