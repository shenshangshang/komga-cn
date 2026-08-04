# 神殇漫画

[![Docker Pulls](https://img.shields.io/docker/pulls/shenshangshang/komga-cn)](https://hub.docker.com/r/shenshangshang/komga-cn)
[![Docker Image](https://img.shields.io/badge/docker-shenshangshang%2Fkomga--cn-blue)](https://hub.docker.com/r/shenshangshang/komga-cn)

## 多用户与注册

- 管理员可在“服务器设置”中关闭注册、开放注册或启用仅邀请注册。
- 邀请模式支持生成 1–30 天有效的一次性邀请链接，并可查看、撤销邀请。
- 新注册用户默认不能看到任何媒体库；管理员可在用户限制中分配可见媒体库。
- 普通用户权限新增“创建媒体库”和“上传漫画压缩包”，与下载、页面浏览、Kobo/KOReader 同步权限独立配置。
- 具备上传权限的用户可上传 CBZ、CBR、ZIP、RAR 或 7Z 文件到其可见的非单本系列；单文件最大 1 GiB。

神殇漫画是基于 [Komga](https://github.com/gotson/komga) 与
[komga-cn](https://github.com/dyphire/komga-cn) 持续开发的中文漫画与电子书媒体服务器。
项目保留 Komga 的用户、权限、阅读进度、OPDS、Kobo/Koreader 同步等能力，并针对中文漫画库、
目录型漫画、嵌套分类、批量下载和现代化界面进行了增强。

## 主要特性

- AURORA 现代化中文界面，支持深浅色主题和移动端布局。
- 支持 CBZ、ZIP、PDF、EPUB、MOBI，以及“文件夹内直接存放图片”的目录型漫画。
- 支持多层目录浏览：顶层目录作为系列，子目录可继续分类，最终进入具体书籍。
- 系列或目录可流式打包下载，目录型漫画可按需生成压缩包。
- 定时扫描、分析与清理，默认可配置为每 15 分钟执行。
- 删除书籍时同步清理数据库和存储文件，并保护嵌套目录中的其他书籍。
- 历史记录显示具体系列名、书籍名和中文操作类型。
- MySQL 主数据库与任务数据支持使用同一数据库。
- 中文拼音索引、繁体转简体、广告页检测和增强型漫画阅读器。

## 快速安装

### 1. 准备 MySQL

当前镜像使用 MySQL。创建数据库和专用用户，字符集建议使用 `utf8mb4`：

```sql
CREATE DATABASE komga CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'komga'@'%' IDENTIFIED BY '请替换为强密码';
GRANT ALL PRIVILEGES ON komga.* TO 'komga'@'%';
FLUSH PRIVILEGES;
```

任务数据与主数据保存在同一个 `komga` 数据库中，无需创建单独的 `komga_tasks`。

### 2. Docker Compose

在 `compose.yaml` 所在目录创建 `.env`，把示例值替换成实际的 MySQL 密码。
该文件只供本机的 Docker Compose 读取，不要上传到 GitHub 或公开分享：

```dotenv
KOMGA_DATABASE_PASSWORD=请替换为数据库密码
```

创建 `compose.yaml`：

```yaml
services:
  komga:
    image: shenshangshang/komga-cn:latest
    container_name: shenshang-manga
    restart: unless-stopped
    ports:
      - "25600:25600"
    environment:
      TZ: Asia/Shanghai
      CHS: "TRUE"
      JAVA_TOOL_OPTIONS: -Xmx1536m
      KOMGA_DATABASE_URL: jdbc:mysql://mysql.example.com:3306/komga
      KOMGA_DATABASE_USERNAME: komga
      KOMGA_DATABASE_PASSWORD: ${KOMGA_DATABASE_PASSWORD}
      KOMGA_DATABASE_POOL_SIZE: "16"
      KOMGA_TASKS_DB_POOL_SIZE: "4"
    volumes:
      - ./config:/config
      - ./data:/data
      - /path/to/comics:/data/Comic
    mem_limit: 2g
```

启动并查看状态：

```bash
docker compose up -d
docker compose ps
docker compose logs -f komga
```

打开 `http://服务器地址:25600`，首次启动后按页面提示创建管理员账户，再将
`/data/Comic` 添加为媒体库目录。

> 镜像默认使用非 root 用户运行。宿主机漫画目录必须允许容器读取；如果需要在网页中删除源文件，
> 还必须授予写入和删除权限。仅在无法正确映射 NAS/CIFS 权限时，才考虑在 Compose 中显式设置
> `user: "0:0"`。

### 3. Docker Run

```bash
docker run -d \
  --name shenshang-manga \
  --restart unless-stopped \
  --env-file .env \
  -p 25600:25600 \
  -e TZ=Asia/Shanghai \
  -e CHS=TRUE \
  -e KOMGA_DATABASE_URL=jdbc:mysql://mysql.example.com:3306/komga \
  -e KOMGA_DATABASE_USERNAME=komga \
  -v "$PWD/config:/config" \
  -v "$PWD/data:/data" \
  -v "/path/to/comics:/data/Comic" \
  shenshangshang/komga-cn:latest
```

生产环境更推荐使用 Compose 和 `.env`，避免密码保留在 Shell 历史中。

## 环境变量

| 变量 | 必需 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `TZ` | 否 | 系统时区 | 建议设置为 `Asia/Shanghai`。 |
| `CHS` | 否 | `FALSE` | `TRUE` 时启用部分内容的繁体转简体处理。 |
| `JAVA_TOOL_OPTIONS` | 否 | 无 | JVM 参数，例如 `-Xmx1536m`。 |
| `KOMGA_CONFIGDIR` | 否 | `/config` | 配置、日志、字体和 Lucene 索引目录。镜像已预设。 |
| `KOMGA_DATABASE_URL` | 是 | 无 | 主数据库 JDBC URL。 |
| `KOMGA_DATABASE_USERNAME` | 是 | 无 | 主数据库用户名。 |
| `KOMGA_DATABASE_PASSWORD` | 是 | 无 | 主数据库密码。 |
| `KOMGA_DATABASE_POOL_SIZE` | 否 | CPU 数量与 8 的较小值 | 主数据库 Hikari 连接池大小。 |
| `KOMGA_DATABASE_MAX_POOL_SIZE` | 否 | `8` | 未显式设置连接池大小时的上限。 |
| `KOMGA_TASKS_DB_POOL_SIZE` | 否 | CPU 数量与 8 的较小值 | 后台任务使用的独立连接池大小。任务数据仍保存在主数据库。 |
| `KOMGA_TASKS_DB_MAX_POOL_SIZE` | 否 | `8` | 未显式设置任务池大小时的上限。 |
| `KOMGA_PREFETCH_PAGES` | 否 | `3` | 阅读器预取页数，允许 `0` 到 `10`。 |

程序内部为网页请求和后台任务保留两个连接池，以避免扫描、分析任务阻塞正常访问；
两者默认连接同一个 MySQL 数据库。普通安装只需配置一组
`KOMGA_DATABASE_URL`、`KOMGA_DATABASE_USERNAME` 和 `KOMGA_DATABASE_PASSWORD`。

为兼容旧部署，仍支持 `KOMGA_TASKS_DB_URL`、`KOMGA_TASKS_DB_USERNAME` 和
`KOMGA_TASKS_DB_PASSWORD`。只有明确需要把任务表放到其他数据库时才设置它们；
未设置时会自动继承主数据库配置。

## 数据目录与备份

- `/config`：应用配置、日志、Lucene 索引和运行数据。
- `/data`：应用数据目录。
- `/data/Comic`：示例漫画库挂载点，可按实际情况增加多个只读或可写挂载。
- MySQL：保存媒体库、系列、书籍、用户、阅读进度、任务和历史记录。

升级前至少备份 MySQL 和 `/config`。漫画源文件应由 NAS 快照或独立备份策略保护。

## 升级与回滚

```bash
docker compose pull
docker compose up -d
curl http://127.0.0.1:25600/actuator/health
```

建议在 Compose 中固定版本或 Git SHA 标签。出现问题时，将 `image` 改回上一个标签并重新执行：

```bash
docker compose up -d
```

数据库迁移由 Flyway 在启动时执行。跨版本回滚前必须同时确认数据库迁移兼容性。

## 从源码构建

要求：

- JDK 21
- Gradle Wrapper 8.14.3
- Node.js 20（版本见 `.nvmrc`）
- npm，依赖版本由 `komga-webui/package-lock.json` 锁定

```bash
git clone https://github.com/shenshangshang/komga-cn.git
cd komga-cn

cd komga-webui
npm ci
npm run lint
npm run test:unit -- --runInBand
cd ..

SKIP_TYPECHECK=true ./gradlew :komga:prepareThymeLeaf :komga:bootJar --no-daemon --max-workers=2
docker build -t shenshangshang/komga-cn:local .
```

## 健康检查

```bash
curl http://127.0.0.1:25600/actuator/health
```

正常响应：

```json
{"status":"UP"}
```

## 上游与许可证

本项目继承上游 Komga/komga-cn 的许可证与第三方依赖约束。发布、再分发或修改前请阅读
仓库中的 [LICENSE](LICENSE)、[PRIVACY.md](PRIVACY.md) 和上游项目说明。
