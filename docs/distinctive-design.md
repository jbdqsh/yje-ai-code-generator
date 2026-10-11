# Distinctive AI Design 接入

来源：https://github.com/houxin0010/distinctive-ai-design

固定提交：`92c68366767b98924fe2719c16659e7f1e93c7af`。
原始文件位于 `.agents/skills/distinctive-ai-design`，保留来源说明及参考文件。
该提交未发现 LICENSE；本项目未为上游文件增加授权，也未将其宣称为原创。
Maven 将 SKILL.md 和 references/review-and-media.md 打包至 classpath 的 design-skill，
运行时不下载更新，不依赖 IDEA 的工作目录查找 skill。

## 网页使用

重启后端后，在 http://localhost:5173 输入业务需求、目标用户和喜好即可。
不需要输入 `$distinctive-ai-design`。

示例：

- 商城：为跑步爱好者创建运动装备商城，优先商品列表、分类筛选及购物车。使用鲜明品牌色，避免宣传页式首屏。
- 博客：为旅行摄影师创建博客，突出文章发现和阅读体验，使用大幅真实照片及清晰正文排版。
- 后台：创建仓库库存后台，优先表格扫描、搜索、低库存告警和批量操作，信息密度高但层级清晰。
- 重设计：保留功能，重新设计视觉，避免当前布局。偏好黑白编辑风格和不对称信息组织。

新增应用会比较三个方向并自动选择；后续修改复用 brief。
“重新设计”“重做视觉”“重新规划”或 redesign 会重新规划。
不会主动改写历史网站。skill 的设计过程不会被要求写入生成页面。

流程：规划 → 生成及保存（Vue 还需构建）→ 桌面及手机截图 → 独立评审 → 最多两轮精修。
前端接收 design-status 阶段事件，整个过程结束才发送 done；生成错误发送 business-error。
评审仅提交图片、业务 brief 和评审参考，不使用生成代码、聊天记忆或以前的评审分数。
截图只能验证视觉，购物、登录、按钮及其他功能仍需独立测试。

## 配置与文件

application.yml 的 `design`：

```yaml
design:
  enabled: true
  review-enabled: true
  max-refinements: 2
  base-url: ${langchain4j.open-ai.chat-model.base-url}
  api-key: ${langchain4j.open-ai.chat-model.api-key}
  model-name: gpt-6.1-sol
  timeout-seconds: 90
  preview-base-url: http://localhost:8123/api/static
  root: ${user.dir}/tmp/design
```

沿用生成模型的地址和密钥，评审模型可独立配置。地址应为 OpenAI Chat Completions API 的基础路径，
例如 `http://host:8080/v1`。密钥继续通过 OPENAI_API_KEY 提供，勿提交至仓库。
必须安装 Chrome；找不到 Chrome 时可设置 CHROME_PATH 为 chrome.exe 的完整路径。

`tmp/design/{appId}/brief.json` 保存可复用方案；每次运行的 UUID 目录保存截图、review JSON、
精修前源码及 dist 备份（排除 node_modules 和 .git）与失败诊断。
Vue 构建输出保存在生成项目的 npm-build.log。
生成源码仍在 tmp/code_output，部署仍在 tmp/code_deploy，本地封面仍由原有 uploads 接口提供。
skill 从 classpath 加载，但生成文件和设计产物的默认目录依然使用 user.dir；移动工作目录时需显式配置。

图片探测不支持、截图失败或评审超时时显示“视觉评审未完成”，保留可用结果。
首次生成或构建失败直接报错。精修生成或构建失败恢复备份；恢复自身失败仍报错。
没有分数成功门槛。风格多样性有所改善，但不能保证每次满足个人审美。

同一应用只接受一次生成；停止按钮关闭 SSE，后端停止后续轮次、工具及模型调用。
已发出的请求或正在执行的工具可能需要返回后才能停止，已生成文件不会自动删除。
停止与精修失败不同，停止时不自动回滚正在编辑的源码；下一次可要求保留文件继续修改。

额外费用：新规划一次文本请求；首次图片能力探测一次小图片请求；每次截图评审一次双图片请求。
最多两轮修正时，最多三次评审及两次额外代码生成；这些均由模型供应商计费。
不会引入付费素材服务。

## 验证

JDK 21 执行：

```text
mvn test -Dtest=Design*Test,ProjectSnapshotTest,AppControllerSseTest,LocalResourceControllerTest,JsonMessageStreamHandlerTest
mvn test -Dtest=ChromeScreenshotSmokeTest -Dscreenshot.smoke=true
```

DesignLiveSmokeTest 为单独的付费集成检查：设置 DESIGN_LIVE_TEST=true、OPENAI_API_KEY、
DESIGN_TEST_BASE_URL 后执行 `mvn test -Dtest=DesignLiveSmokeTest`。
它使用真实规划、提示组合、独立 HTML 生成、Chrome 截图和图片评审，产物保存到
target/verification/design-live，不写入用户应用或聊天数据库。
它不替代网页端真实 Vue 工具链、数据库、Redis 和 SSE 的端到端验收。
