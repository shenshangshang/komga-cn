# 神殇漫画（Komga 中文增强版）

神殇漫画是面向中文漫画库的 Komga 增强镜像，提供 AURORA 现代化界面、目录型漫画、
多层目录分类、系列/目录打包下载、定时扫描与清理、MySQL 存储和中文历史记录。

源码与完整说明：
[github.com/shenshangshang/komga-cn](https://github.com/shenshangshang/komga-cn)

## 多用户能力

- 支持关闭、开放或仅邀请注册；邀请链接一次性使用并可设置有效期。
- 管理员可为每个用户分配可见媒体库。
- 可独立授予普通用户创建媒体库、上传漫画压缩包、下载文件和页面浏览等权限。

## 支持内容

- CBZ、ZIP、PDF、EPUB、MOBI
- 图片文件夹直接识别为漫画，无需预先压缩
- 多层目录浏览与嵌套分类
- 系列和目录流式打包下载
- 删除源文件、数据库清理与兄弟目录保护
- 可配置的定时扫描、分析和清理
- MySQL 主数据与任务数据共库

## Docker Compose

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

在 `compose.yaml` 同目录创建 `.env`，把示例值替换成实际的 MySQL 密码。
该文件仅供本机 Docker Compose 读取，请勿上传或公开分享：

```dotenv
KOMGA_DATABASE_PASSWORD=请替换为数据库密码
```

启动：

```bash
docker compose up -d
```

访问 `http://服务器地址:25600`，将 `/data/Comic` 添加为媒体库。

## 主要环境变量

| 变量 | 说明 |
| --- | --- |
| `CHS` | `TRUE` 时启用部分内容的繁体转简体处理。 |
| `KOMGA_DATABASE_URL` | 主数据库 JDBC URL。 |
| `KOMGA_DATABASE_USERNAME` | 主数据库用户名。 |
| `KOMGA_DATABASE_PASSWORD` | 主数据库密码。 |
| `KOMGA_DATABASE_POOL_SIZE` | 主数据库连接池大小。 |
| `KOMGA_TASKS_DB_POOL_SIZE` | 后台任务独立连接池大小；任务数据仍保存在主数据库。 |
| `KOMGA_PREFETCH_PAGES` | 阅读器预取页数，范围 `0` 到 `10`。 |
| `JAVA_TOOL_OPTIONS` | JVM 内存等运行参数。 |

完整变量、MySQL 初始化、权限、备份、升级、回滚和源码构建说明请查看 GitHub README。

普通安装只需配置一组 `KOMGA_DATABASE_URL`、`KOMGA_DATABASE_USERNAME` 和
`KOMGA_DATABASE_PASSWORD`。网页请求与后台任务虽然使用两个连接池，但默认连接同一个
MySQL 数据库，不需要重复填写数据库地址和账户。

## 数据与权限

- `/config`：配置、日志和索引。
- `/data`：应用数据。
- 漫画目录需至少可读；若要在网页中删除源文件，容器还需要写入和删除权限。
- 生产环境请备份 MySQL、`/config` 和漫画源文件。

健康检查：

```bash
curl http://127.0.0.1:25600/actuator/health
```
