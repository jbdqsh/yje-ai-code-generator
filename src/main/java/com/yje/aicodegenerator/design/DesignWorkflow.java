package com.yje.aicodegenerator.design;

import cn.hutool.json.JSONUtil;
import com.yje.aicodegenerator.constant.AppConstant;
import com.yje.aicodegenerator.model.enums.CodeGenTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class DesignWorkflow {
    public static final String STATUS_PREFIX = "\u001eDESIGN:";
    private final DesignProperties properties;
    private final DesignPlanningService planning;
    private final DesignPromptComposer composer;
    private final DesignReviewService reviewer;
    private final GenerationRunRegistry runs;
    private final DesignScreenshotService screenshots;

    public Flux<String> run(long appId, CodeGenTypeEnum type, String prompt,
                            Function<String, Flux<String>> generate) {
        return Flux.defer(() -> {
            var lease = runs.acquire(appId);
            Path run = Path.of(properties.getRoot(), Long.toString(appId), UUID.randomUUID().toString());
            Flux<String> workflow;
            if (!properties.isEnabled()) {
                workflow = Flux.defer(() -> generate.apply(prompt));
            } else {
                workflow = status("planning", "正在比较设计方向并制定视觉方案")
                        .concatWith(Mono.fromCallable(() -> planning.prepare(appId, prompt))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMapMany(brief -> status("generation", "正在按设计方案生成网站")
                                        .concatWith(Flux.defer(() -> generate.apply(composer.compose(prompt, brief))))
                                        .concatWith(review(appId, type, brief, run, 0, generate))));
            }
            return workflow.doFinally(signal -> runs.release(appId, lease));
        });
    }

    private Flux<String> review(long appId, CodeGenTypeEnum type, DesignBrief brief, Path run,
                                int refinements, Function<String, Flux<String>> generate) {
        return Flux.defer(() -> {
            if (!properties.isReviewEnabled()) {
                return status("skipped", "视觉评审未完成：自动评审已关闭");
            }
            return status("review", "正在截图并评审桌面和手机布局")
                    .concatWith(Mono.fromCallable(() -> captureAndReview(appId, type, brief, run, refinements))
                            .subscribeOn(Schedulers.boundedElastic())
                            .map(Optional::of)
                            .onErrorResume(error -> {
                                log.warn("Visual review unavailable for app {}: {}", appId, error.getClass().getSimpleName());
                                return recordWarning(run, error).thenReturn(Optional.empty());
                            })
                            .flatMapMany(outcome -> {
                                if (outcome.isEmpty()) return status("skipped", "视觉评审未完成：截图或评审服务不可用，已保留生成结果");
                                DesignReview result = outcome.get();
                                if (!result.improvementNeeded() || result.issues().isEmpty()) {
                                    return status("complete", "视觉评审完成；交互功能仍需单独验证");
                                }
                                int limit = Math.max(0, Math.min(2, properties.getMaxRefinements()));
                                if (refinements >= limit) {
                                    return status("complete", "已达到精修次数上限，剩余问题已记录");
                                }
                                return refine(appId, type, brief, result, run, refinements, generate);
                            }));
        });
    }

    private Flux<String> refine(long appId, CodeGenTypeEnum type, DesignBrief brief, DesignReview result,
                                Path run, int iteration, Function<String, Flux<String>> generate) {
        Path project = project(appId, type);
        Path backup = run.resolve("backup-" + (iteration + 1));
        ProjectSnapshot snapshots = new ProjectSnapshot();
        var restored = new java.util.concurrent.atomic.AtomicBoolean();
        return status("refining", "正在进行第 " + (iteration + 1) + " 轮视觉精修")
                .concatWith(Mono.fromCallable(() -> {
                    snapshots.backup(project, backup);
                    return true;
                }).subscribeOn(Schedulers.boundedElastic()).flatMapMany(ignored ->
                        Flux.defer(() -> generate.apply(composer.compose(
                                "保留业务功能，在现有文件基础上仅修正以下视觉问题，不重建整个项目：\n"
                                        + JSONUtil.toJsonStr(result.issues()), brief)))
                                .onErrorResume(error -> Mono.fromCallable(() -> {
                                    snapshots.restore(backup, project);
                                    restored.set(true);
                                    Files.writeString(run.resolve("refinement-failure-" + iteration + ".txt"),
                                            error.getClass().getSimpleName());
                                    return true;
                                }).subscribeOn(Schedulers.boundedElastic()).thenMany(
                                        status("restored", "精修失败，已恢复上一版可用网站；视觉验证未完成")))
                                .concatWith(Flux.defer(() -> restored.get()
                                        ? Flux.empty() : review(appId, type, brief, run, iteration + 1, generate)))));
    }

    private DesignReview captureAndReview(long appId, CodeGenTypeEnum type, DesignBrief brief,
                                          Path run, int iteration) throws Exception {
        if (!reviewer.probeVision()) throw new IllegalStateException("Model does not support image review");
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        Files.createDirectories(run);
        String url = properties.getPreviewBaseUrl().replaceAll("/+$", "") + "/" + type.getValue()
                + "_" + appId + (type == CodeGenTypeEnum.VUE_PROJECT ? "/dist/index.html" : "/index.html");
        Path desktop = run.resolve("desktop-" + iteration + ".png");
        Path mobile = run.resolve("mobile-" + iteration + ".png");
        screenshots.capture(url, desktop, 1440, 900);
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        screenshots.capture(url, mobile, 390, 844);
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        DesignReview result = reviewer.review(brief, List.of(desktop, mobile));
        Files.writeString(run.resolve("review-" + iteration + ".json"), JSONUtil.toJsonPrettyStr(result));
        return result;
    }

    private Mono<Boolean> recordWarning(Path run, Throwable error) {
        return Mono.fromCallable(() -> {
            Files.createDirectories(run);
            Files.writeString(run.resolve("review-unavailable.txt"), error.getClass().getSimpleName());
            return true;
        }).subscribeOn(Schedulers.boundedElastic()).onErrorReturn(false);
    }

    Path project(long appId, CodeGenTypeEnum type) {
        return Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, type.getValue() + "_" + appId);
    }

    private Flux<String> status(String stage, String message) {
        return Flux.just(STATUS_PREFIX + JSONUtil.toJsonStr(Map.of("stage", stage, "message", message)));
    }
}
