# 上游同步机制

本仓库基于 [dyphire/komga-cn](https://github.com/dyphire/komga-cn)（上游）定制开发。
通过 GitHub Action 自动同步上游更新，减少手动合并成本。

## 上游与分支映射

| 项目 | 仓库 | 默认分支 |
|------|------|----------|
| 上游 | `dyphire/komga-cn` | `master` |
| 本仓库 | `shenshangshang/komga-cn` | `main` |

Action 将 `upstream/master` 合并到 `main`。

## 自动同步行为

Workflow 文件：`.github/workflows/sync-upstream.yml`

- **定时检查**：每天北京时间 08:00（UTC 00:00）运行一次。
- **手动触发**：仓库 Actions 页面 → "Sync Upstream" → Run workflow。
- **无冲突**：自动创建/更新 PR（分支 `sync-upstream`，标签 `sync-upstream`），
  review 后合并到 main。
- **有冲突**：自动创建/评论 issue（标签 `sync-conflict`），列出冲突文件，
  需人工解决。

> 注意：自动同步只能可靠处理无冲突的合并。当上游改动触及本仓库的定制区域
> （多用户注册、AURORA 界面、MySQL 支持、定时扫描、上传等）时会产生冲突，
> 必须人工解决。

## 冲突解决流程

收到 `sync-conflict` issue 通知时：

```bash
git clone https://github.com/shenshangshang/komga-cn.git
cd komga-cn
git remote add upstream https://github.com/dyphire/komga-cn.git
git fetch upstream master
git merge upstream/master
# 解决冲突文件：保留本仓库定制功能的同时采纳上游修复
git add .
git commit
git push origin main
```

解决并推送后关闭 conflict issue。下一次定时检查会重新尝试同步。

## 同步基点

首次建立自动同步时，本仓库 main 已是上游 master 的超集：
上游 HEAD `da719977` 已包含在 main 中，main 领先 97 个定制提交。
因此后续同步均为增量合并上游新增提交，无需首次人工同步。
