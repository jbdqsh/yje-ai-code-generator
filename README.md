# YJE AI 应用生成平台

本项目由 yje 基于程序员鱼皮的 AI 零代码应用生成平台修改维护。

## 本地开发

- 单体后端：根目录 Maven 项目，Java 包名 `com.yje.aicodegenerator`。
- 启动类：`YjeAiCodeGeneratorApplication`。
- 前端：`yje-ai-code-generator-frontend`，执行 `npm install` 和 `npm run dev`。
- 环境：JDK 21、Node.js 22.12+（22.x）、MySQL 8、Redis 7；截图功能需要 Chrome。
- 执行 `sql/create_table.sql` 创建 `yje_ai_code_generator` 数据库。
- 在 `src/main/resources/application-local.yml` 配置数据库、Redis 和自己的模型 API Key；不要提交密钥。
- 后端执行 `mvnw.cmd spring-boot:run`，默认端口 8123，接口前缀 `/api`。
- 原数据库已有数据时需要迁移到新库；改名不会自动迁移数据。
- `yu-ai-code-mother-microservice` 为保留的原始微服务版本，本次仅修改单体后端与前端。

## 原项目介绍与来源
### 本地部署与文件存储

部署地址默认为 `http://localhost:8123/api/deploy/{deployKey}/`，后端直接提供
`tmp/code_deploy` 下的部署文件，无需 Nginx。前端的 `VITE_DEPLOY_DOMAIN` 与后端
`code.deploy-host` 应保持一致。

上传文件统一保存在项目根目录的 `uploads/`，截图位于 `screenshots/年/月/日/`，
架构图位于 `mermaid/`。`storage.local.root` 可修改保存目录，
`storage.local.public-base-url` 配置公开访问前缀（默认 `http://localhost:8123/api/files`）。
文件不会上传到 COS，且 `uploads` 中的数据不会提交到 Git。

封面在部署完成后异步生成，需要已安装 Chrome，无需下载 ChromeDriver。
非默认路径安装时，可在后端运行环境中设置 `CHROME_PATH` 指向 Chrome 可执行文件。
已有应用重新点击部署后会更新封面，截图失败会在后端记录日志，不影响网站访问。
这些默认链接仅供本机使用；部署到其他机器时需同步修改后端和前端公开地址。


> 作者：[程序员鱼皮](https://yuyuanweb.feishu.cn/wiki/Abldw5WkjidySxkKxU2cQdAtnah)
>
> 本项目为教学项目，提供完整视频教程 + 文字教程 + 简历写法 + 面试题解 + 答疑服务，帮你提升项目能力，给简历增加亮点！
>
> ⭐️ 加入项目系列学习：[加入编程导航](https://www.codefather.cn/vip)


## 一、项目介绍

> 视频介绍：https://www.bilibili.com/video/BV1XsbSznEs4

这是一套以 **AI 开发实战 + 后端架构设计** 为核心的项目教程，基于 Spring Boot 3 + LangChain4j + Vue 3 开发对标大厂的 **企业级 AI 代码生成平台**，带大家掌握新时代程序员必知必会的 AI 智能体开发、AI 工作流等前沿技术，大幅提升求职竞争力！

![](https://pic.yupi.icu/1/1753332293578-0ccc2a53-6d98-41a7-b714-16fa6a7f095f.png)



### 4 大核心能力

1）智能代码生成：用户输入需求描述，AI 自动分析并选择合适的生成策略，通过工具调用生成代码文件，采用流式输出让用户实时看到 AI 的执行过程。

![](https://pic.yupi.icu/1/1753332332820-9ec614de-65a2-496d-b9b2-dc89c20d06c9.png)



2）可视化编辑：生成的应用将实时展示，可以进入编辑模式，自由选择网页元素并且和 AI 对话来快速修改页面，直到满意为止。

![](https://pic.yupi.icu/1/1753332451827-220a1df9-ea60-4646-bea0-64e5f73d15fe.png)



3）一键部署分享：可以将生成的应用一键部署到云端并自动截取封面图，获得可访问的地址进行分享，同时支持完整项目源码下载。

![](https://pic.yupi.icu/1/1753332366033-187b00b1-8609-42b1-ba80-cf58bdb0e970.png)

查看精选案例：

![](https://pic.yupi.icu/1/1753332637580-d9e92c36-789d-4ded-b03b-16a1ac61dd27.png)



4）企业级管理：提供用户管理、应用管理、系统监控、业务指标监控等后台功能，管理员可以设置精选应用、监控 AI 调用情况和系统性能。

![](https://pic.yupi.icu/1/1753281175326-d7ecfcb9-f034-4893-8e13-be0c5dcdacf6.png)

![](https://pic.yupi.icu/1/1753333524767-c89b8d1b-bc57-4094-ace7-1b37876f3f0b.png)



![](https://pic.yupi.icu/1/1753332482457-e6b13118-e150-45e5-bf36-6cf355cbec19.png)



当你学会这个项目后，你不仅能开发 AI 代码生成器，更能灵活开发各种复杂的 AI 应用：AI 写作助手、AI 设计工具、AI 数据分析平台、AI PPT 制作大师，尽情发挥自己的想象力吧！



### 为什么做这个项目？

1）大厂都在做：如今各大厂都在疯狂布局 AI 编程赛道，无论是网页端的 AI 应用生成器，还是客户端 IDE 和 AI 编程插件，已经成为风口。

2）找工作好用：随着 AI 发展，企业对 AI 开发者需求激增，掌握 AI 应用开发的程序员在求职时极具优势。

3）技术值得学：此类项目的实现不仅需要 AI 智能体 / 工作流开发技术，还需要各种后端技术和架构设计能力。

![](https://pic.yupi.icu/1/1753325705083-12fe403e-93ba-4289-87cd-61ec83b57d5e.png)



## 二、项目优势

本项目紧跟 AI 时代、选题新颖、**对标大厂** 产品业务、技术丰富。区别于增删改查的烂大街项目，鱼皮会带你实战大量新技术和企业应用场景，掌握层层递进的系统设计、项目扩展和优化方案，帮你成为 AI 时代企业的香饽饽，给你的简历和求职大幅增加竞争力！

微服务 AI 全栈项目，技术丰富，玩透 AI 开发~

![](https://pic.yupi.icu/1/1753272108623-c211ff4f-35ff-4f2c-bd5d-3a39a13d8f1c.png)

业务场景真实，实践大量企业解决方案：

![](https://pic.yupi.icu/1/1753290346736-47093142-ff4f-4b1d-8523-f6dea8de7e3c.png)

鱼皮给大家讲的是 **通用的项目开发方法、企业级架构设计套路和最新的 AI 应用开发技术**，从这个项目中你可以学到：

- 如何基于 LangChain4j 构建 AI 应用，实现真正的 AI 驱动业务？
- 如何基于 LangGraph4j 实现 AI 工作流，实现复杂的 AI 智能体？
- 如何设计 AI 工具调用机制，让 AI 智能生成复杂项目？
- 如何实现 AI 流式输出 + 响应式编程，提升并发性能？
- 如何使用 Spring Cloud Alibaba + Dubbo 实现微服务架构？
- 如何设计复杂的 AI 对话记忆机制，实现多租户的 AI 服务架构？
- 如何利用 AI Vibe Coding 和代码生成引擎，快速实现企业级系统？
- 如何结合 Redis + Caffeine 构建高性能多级 AI 服务缓存？
- 如何从性能、安全性、稳定性、成本等角度全方面优化项目？
- 如何巧用多种设计模式，打造可扩展的企业级架构？
- 如何构建企业级监控体系，实时掌握系统和 AI 服务的状态？
- 如何实现动态网站浏览、网站截图和部署服务？

此外，还能学会很多 AI 编程、系统架构设计、技术方案对比的方法，提升排查问题、自主解决 Bug 的能力。鱼皮还给大家提供了大量的项目扩展点，有能力的同学可以进一步拉开和别人的区分度，无限进步！



### 鱼皮系列项目优势

鱼皮的原创项目以 **实战** 为主，用 **全程直播** 的方式 **从 0 到 1** 带做，从需求分析、技术选型、项目设计、项目初始化、Demo 编写、前后端开发实现、项目优化、部署上线等，每个环节我都 **从理论到实践** 给大家讲的明明白白、每个细节都不放过！

比起看网上的教程学习，鱼皮项目系列的优势：从学知识 => 实践项目 => 复习笔记 => 项目答疑 => 简历写法 => 面试题解的一条龙服务

![](https://pic.yupi.icu/1/image-20250724150852970.png)

编程导航已有 **近 20 套项目教程！** 每个项目的学习重点不同，几乎全都是前端 + 后端的 **全栈项目** 。

详细请见：[https://codefather.cn/course](https://www.codefather.cn/course)（在该页面右侧有教程推荐和学习建议）

往期项目介绍视频：[https://bilibili.com/video/BV1YvmbYbEgS](https://www.bilibili.com/video/BV1YvmbYbEgS/)

![](https://pic.yupi.icu/1/1753338231169-664e7486-0f52-4153-a28b-0a8757012009.png)



## 三、更多介绍

功能模块：

![](https://pic.yupi.icu/1/image%20(1).png)

核心业务流程：

![](https://pic.yupi.icu/1/image-20250724145913756.png)

架构设计：

![](https://pic.yupi.icu/1/AI%E5%BA%94%E7%94%A8%E7%94%9F%E6%88%90%E5%B9%B3%E5%8F%B0%E6%9E%B6%E6%9E%84%E5%9B%BE.png)


## 本地设计生成接入

已接入 Distinctive AI Design：自动比较设计方向、生成网站、桌面和手机截图评审，以及最多两轮视觉精修。
重启后端后从网页正常输入需求即可。安装来源、使用示例、配置、调用费用与降级限制见
[接入说明](docs/distinctive-design.md)，实际验证结果见 [验证记录](docs/design-verification.md)。

## 第一期免费看

第一期是公开讲解，给大家介绍项目背景、项目功能、技术选型、架构设计、教程计划等。

视频地址：[https://www.bilibili.com/video/BV1Eq5DzcE9o](https://www.bilibili.com/video/BV1XsbSznEs4/)
​	

## 加入项目学习

编程导航已有 **近 20 套项目教程**！每个项目的学习重点不同，几乎全都是前端 + 后端的 **全栈** 项目 。

![](https://pic.yupi.icu/1/wechat_2025-07-24_115207_359.png)

欢迎加入 [编程导航](https://mp.weixin.qq.com/s/I1oD6pAaWBvGLyFDT9AgvA?token=1925632390&lang=zh_CN)，加入后不仅可以全程跟学本项目，往期 [10+ 套原创项目教程](https://mp.weixin.qq.com/s/omIazLMQlTo9M3jFFH7NzQ?token=70787607&lang=zh_CN) 也都可以无限回看。还能享受更多原创技术资料、学习和求职指导、上百场面试回放视频，开启你的编程起飞之旅~

🧧 助力新项目学习，给大家发放 **限时编程导航优惠券**，扫码即可领券加入。加入三天内不满意可全额退款，欢迎加入体验，名额有限，速来学习！

<img width="404" alt="image" src="https://github.com/user-attachments/assets/56411098-b60e-4267-8ba2-4ebc5d416afc" />

1 天不到 1 块钱，绝对是对自己最值的投资！成为编程导航会员后，可以解锁 10 多套项目的教程和资料，PC 网站和 APP 都可以学习，如图：

![](https://pic.yupi.icu/1/image-20250120113756426-20250422160856746.png)
