# 接入验证记录

日期：2026-10-10；环境：Windows、JDK 21、Node 23、本地 Chrome。

## 已通过

- 前端 `npm.cmd run build`：TypeScript 检查及 Vite 生产构建通过，保留原有大包警告。
- DesignServicesTest：skill classpath 加载、brief 持久化/复用/重规划、输出协议保留、无图片能力时终止评审请求。
- DesignModelClientTest：瞬时探测失败不缓存、再次探测成功及缓存、JSON 解析。
- DesignWorkflowTest：规划/生成/评审阶段顺序、三种输出类型、图片能力降级、评审异常降级、生成失败不截图、最多两轮、精修失败恢复、并发拒绝、取消释放、取消截图后不调用评审。
- ProjectSnapshotTest：恢复源码及 dist、删除失败轮次新增文件、排除并保留 node_modules。
- AppControllerSseTest：阶段事件独立、done 位于最后、business-error 后结束、心跳及取消、不重复订阅生成服务。
- LocalResourceControllerTest / JsonMessageStreamHandlerTest：本地文件及部署访问、现有工具结果处理回归。
- ChromeScreenshotSmokeTest：真实 Chrome，非空内容、1440×900 与 390×844 输出；CDP 另外校验真实 CSS viewport 宽度。
- JAR 打包成功，确认 BOOT-INF/classes/design-skill/SKILL.md 及评审参考存在。
- 现有前端 5173、部署 /api/deploy/45al0C/、Vue 预览及本地封面图片接口返回 HTTP 200。

## 真实模型案例

使用 IDEA 已配置的密钥测试中转 `/v1/chat/completions`，模型为 gpt-6.1-sol；未输出密钥。
第一次完整 HTML 生成在 120 秒超时。缩小为主要业务页面、将测试超时增至 180 秒后，
三个案例的规划和独立 HTML 生成均成功，产物位于 `target/verification/design-live`：

| ID | 业务 | 结构 |
| --- | --- | --- |
| 100 | 运动装备商城 | 分类、商品目录、筛选、尺寸与购物袋 |
| 101 | 旅行摄影博客 | 摄影主图、衬线标题、文章阅读与目的地发现 |
| 102 | 库存后台 | 操作导航、库存指标、告警、表格与搜索 |

原始 CLI 截图为 desktop.png / mobile.png；它们暴露了 Chrome 的最小布局宽度问题，
不能用作精确手机布局验收。实现已改用 CDP 显式设置 CSS viewport。
DesignArtifactScreenshotTest 在无额外模型费用的情况下重新捕获生成页面，
最终截图为 viewport-1440.png / viewport-390.png。截图服务会等待首屏可见懒加载图片。
人工查看六张最终截图：商城为目录及商品图片，博客为摄影与阅读布局，后台为密集操作表格，
没有统一套用米色首屏或同一构图。商城及博客手机布局可读；后台手机保留宽表格，
需要横向浏览，操作效率仍需另行确认。商品素材与真实商品的对应关系仍需人工核对。

小图能力探测第一次通过，重试时未通过语义确认，因此本次三个案例自动跳过图片评审。
这说明中转图片能力尚未得到稳定确认，并不证明模型本身不支持图片。
视觉评审及两轮纠正的编排已通过模拟测试，但**真实模型的图片评审及修正闭环未验收完成**。
能力或网络不满足时，网页会保留结果并显示“视觉评审未完成”。

## 验证边界

真实案例使用相同 skill、规划、提示组合及截图模块，但通过独立 HTML 请求生成，
没有写入用户 MySQL / Redis 聊天历史，没有替代网页端 Vue 工具调用的端到端验收。
尚未对生成案例的购物、登录、筛选等交互做完整功能测试，截图也不能证明交互正确。
没有自动修改、重新部署或重新生成用户历史网站。正在运行的 IDEA 后端未被强制重启；
需要重启后端加载新代码，再从现有网页验证完整业务链路。

点击停止后会终止后续步骤；SSE 心跳帮助服务端发现连接关闭。
已发出的模型请求、正在执行的单个工具可能先返回，不能承诺供应商立即停止计费。
取消不会自动恢复精修源码；精修生成/构建失败则自动恢复上一版。
