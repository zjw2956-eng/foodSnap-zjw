# 第一步：创建项目目录（关键！避免权限问题）

# 在 C 盘用户目录创建（Docker Desktop 默认共享，最安全！）
cd C:\Users\$env:USERNAME
mkdir rocketmq-5.4.0
cd rocketmq-5.4.0

# 创建必要子目录
mkdir conf, logs, store, html

# 在rocketmq-5.4.0目录下创建 docker-compose.yml（复制粘贴即可）

version: '3.8'  //通过docker-compose --version命令查看docker-compose版本，修改这个值
services:
  rmqnamesrv:
    image: apache/rocketmq:5.4.0
    container_name: rmqnamesrv
    ports:
      - "9876:9876"
    volumes:
      - ./logs/namesrv:/home/rocketmq/logs
      - ./store/namesrv:/home/rocketmq/store
    command: sh mqnamesrv
    networks:
      rmq-net:
        aliases:
          - namesrv

  rmqbroker:
    image: apache/rocketmq:5.4.0
    container_name: rmqbroker
    ports:
      - "10909:10909"  # HA 端口
      - "10911:10911"  # 服务端口
      - "10912:10912"  # VIP 通道端口
    volumes:
      - ./logs/broker:/home/rocketmq/logs
      - ./store/broker:/home/rocketmq/store
      - ./conf/broker.conf:/etc/rocketmq/broker.conf
    environment:
      NAMESRV_ADDR: "namesrv:9876"
      JAVA_OPTS: "-Duser.home=/home/rocketmq"
      JAVA_OPT_EXT: "-server -Xms256m -Xmx256m -Xmn128m"
    depends_on:
      - rmqnamesrv
    command: sh mqbroker -c /etc/rocketmq/broker.conf
    networks:
      rmq-net:
        aliases:
          - broker

  rmqconsole:
    image: apacherocketmq/rocketmq-console:2.0.0
    container_name: rmqconsole
    ports:
      - "8098:8080"
    environment:
      JAVA_OPTS: "-Drocketmq.namesrv.addr=namesrv:9876 -Dcom.rocketmq.sendMessageWithVIPChannel=false"
    depends_on:
      - rmqnamesrv
    networks:
      rmq-net:
        aliases:
          - console

networks:
  rmq-net:
    driver: bridge

# 创建 conf/broker.conf
# 在 PowerShell 中执行（写入 broker 配置）
@"
brokerClusterName = DefaultCluster
brokerName = broker-a
brokerId = 0
# ⚠️ 重要：brokerIP1 必须是宿主机可路由的 IP，不能用 host.docker.internal
# host.docker.internal 是容器访问宿主机的 DNS，方向是「容器 → 宿主机」
# 但 brokerIP1 是 broker 告诉 NameServer「外部怎么访问我」，方向是「外部 → 容器」
# 用 host.docker.internal 会导致外部 Java 客户端拿到的地址无法解析
brokerIP1 = 127.0.0.1
listenPort = 10911
namesrvAddr = namesrv:9876
autoCreateTopicEnable = true
autoCreateSubscriptionGroup = true
flushDiskType = ASYNC_FLUSH
storePathRootDir = /home/rocketmq/store
storePathCommitLog = /home/rocketmq/store/commitlog
storePathConsumeQueue = /home/rocketmq/store/consumequeue
storePathIndex = /home/rocketmq/store/index
storeCheckpoint = /home/rocketmq/store/checkpoint
abortFile = /home/rocketmq/store/abort
diskMaxUsedSpaceRatio = 95
"@ | Out-File -FilePath conf\broker.conf -Encoding utf8

# ⚠️ 修改 broker.conf 后必须重启 broker 容器才生效
docker-compose restart rmqbroker

# 第三步：启动服务（3 条命令搞定）
# 1. 启动所有服务（-d 后台运行）
docker-compose up -d

# 2. 查看启动状态（等待 10 秒让服务初始化）
Start-Sleep -Seconds 10
docker-compose ps

# 3. 检查关键日志（确认无报错）
docker-compose logs rmqbroker | Select-String "Register broker"
# 应看到：Register broker to name server namesrv:9876 successful

# 成功标志：
NAME          COMMAND                  STATUS
rmqnamesrv    "sh mqnamesrv"           Up 9876/tcp
rmqbroker     "mqbroker -c /etc/..."   Up 10909-10912/tcp
rmqconsole    "java -jar ..."          Up 8080/tcp

# 第四步：验证部署（3 重验证）
验证项	操作	预期结果
1. 控制台访问	浏览器打开 http://localhost:8098	看到 RocketMQ Console 界面
2. Broker 注册	控制台 → 集群 → Broker	看到 broker-a 节点（地址含 127.0.0.1）
3. 本地连接测试	用 Java/Python 客户端连 127.0.0.1:9876	能创建 Topic 并收发消息

# 核心原理总结（为什么这样写）
配置项	            作用	          熟悉的 Docker 命令类比
networks: rmq-net	创建专属网络	docker network create rmq-net
aliases: [namesrv]	服务别名（容器内 DNS）	--network-alias namesrv
depends_on	启动顺序依赖	手动先启动 namesrv 再启动 broker
volumes 挂载日志/存储	持久化数据	-v ./logs:/home/rocketmq/logs
brokerIP1 = 127.0.0.1	宿主机可路由的 IP，Java 端能直接连	无直接对应（按部署环境调整）
