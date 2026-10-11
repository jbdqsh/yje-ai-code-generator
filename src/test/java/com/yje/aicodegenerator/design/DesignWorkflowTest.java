package com.yje.aicodegenerator.design;

import com.yje.aicodegenerator.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DesignWorkflowTest {
    @TempDir Path temp;
    DesignProperties properties;
    DesignReviewService reviewer;
    DesignScreenshotService screenshots;
    DesignWorkflow workflow;
    final CodeGenTypeEnum type = CodeGenTypeEnum.VUE_PROJECT;

    @BeforeEach void setup() throws Exception {
        properties = new DesignProperties();
        properties.setRoot(temp.resolve("design").toString());
        var planner = mock(DesignPlanningService.class);
        when(planner.prepare(anyLong(), anyString())).thenReturn(mock(DesignBrief.class));
        var composer = mock(DesignPromptComposer.class);
        when(composer.compose(anyString(), any())).thenAnswer(call -> call.getArgument(0));
        reviewer = mock(DesignReviewService.class);
        when(reviewer.probeVision()).thenReturn(true);
        when(reviewer.review(any(), anyList())).thenReturn(new DesignReview(List.of(), "ok", false));
        screenshots = mock(DesignScreenshotService.class);
        workflow = spy(new DesignWorkflow(properties, planner, composer, reviewer, new GenerationRunRegistry(), screenshots));
        Path project = temp.resolve("project");
        Files.createDirectories(project.resolve("dist"));
        Files.writeString(project.resolve("dist/index.html"), "working");
        doReturn(project).when(workflow).project(anyLong(), any());
    }

    private List<String> run(java.util.function.Function<String, Flux<String>> generator) {
        return workflow.run(9001, type, "Build a shop", generator).collectList().block(Duration.ofSeconds(10));
    }

    @Test void stagesFollowGenerationAndReview() {
        var output = run(prompt -> Flux.just("code"));
        assertTrue(output.get(0).contains("planning"));
        assertTrue(output.get(1).contains("generation"));
        assertEquals("code", output.get(2));
        assertTrue(output.get(3).contains("review"));
        assertTrue(output.get(4).contains("complete"));
    }

    @Test void unsupportedVisionSkipsScreenshots() throws Exception {
        when(reviewer.probeVision()).thenReturn(false);
        assertTrue(run(prompt -> Flux.just("code")).getLast().contains("skipped"));
        verifyNoInteractions(screenshots);
    }

    @Test void reviewTimeoutDeliversGeneratedResult() {
        when(reviewer.review(any(), anyList())).thenThrow(new IllegalStateException("timeout"));
        var output = run(prompt -> Flux.just("code"));
        assertTrue(output.contains("code"));
        assertTrue(output.getLast().contains("skipped"));
    }

    @Test void generationFailureDoesNotScreenshot() {
        assertThrows(RuntimeException.class, () -> run(prompt -> Flux.error(new IllegalStateException("build failed"))));
        verifyNoInteractions(screenshots);
    }

    @Test void refinementLimitIsTwoEvenWhenConfiguredHigher() {
        properties.setMaxRefinements(9);
        when(reviewer.review(any(), anyList())).thenReturn(new DesignReview(List.of(
                new DesignReview.Issue("mobile", "overflow", "unreadable", "wrap")), "fix", true));
        AtomicInteger calls = new AtomicInteger();
        var output = run(prompt -> { calls.incrementAndGet(); return Flux.just("code"); });
        assertEquals(3, calls.get());
        verify(reviewer, times(3)).review(any(), anyList());
        assertTrue(output.getLast().contains("上限"));
    }

    @Test void failedRefinementRestoresSourceAndBuild() throws Exception {
        when(reviewer.review(any(), anyList())).thenReturn(new DesignReview(List.of(
                new DesignReview.Issue("mobile", "overflow", "unreadable", "wrap")), "fix", true));
        AtomicInteger calls = new AtomicInteger();
        var output = run(prompt -> {
            if (calls.incrementAndGet() == 1) return Flux.just("code");
            try { Files.writeString(temp.resolve("project/dist/index.html"), "broken"); }
            catch (Exception error) { throw new RuntimeException(error); }
            return Flux.error(new IllegalStateException("build failed"));
        });
        assertEquals("working", Files.readString(temp.resolve("project/dist/index.html")));
        assertTrue(output.getLast().contains("restored"));
        verify(reviewer, times(1)).review(any(), anyList());
    }

    @Test void concurrentRunRejectedAndCancellationReleasesLease() {
        properties.setEnabled(false);
        var subscription = workflow.run(9002, type, "first", prompt -> Flux.never()).subscribe();
        assertThrows(RuntimeException.class, () -> workflow.run(9002, type, "second", prompt -> Flux.just("code")).blockLast());
        subscription.dispose();
        assertEquals("code", workflow.run(9002, type, "third", prompt -> Flux.just("code")).blockLast());
    }

    @Test void cancellationDoesNotStartReview() throws Exception {
        var subscription = workflow.run(9003, type, "shop", prompt -> Flux.never()).subscribe();
        subscription.dispose();
        verifyNoInteractions(reviewer, screenshots);
    }

    @Test void allOutputTypesReceiveComposedBriefWithoutChangingRouting() {
        properties.setReviewEnabled(false);
        for (var outputType : CodeGenTypeEnum.values()) {
            var messages = workflow.run(9100 + outputType.ordinal(), outputType, "shop", prompt -> {
                assertEquals("shop", prompt);
                return Flux.just("saved " + outputType.getValue());
            }).collectList().block(Duration.ofSeconds(10));
            assertTrue(messages.contains("saved " + outputType.getValue()));
            assertTrue(messages.getLast().contains("skipped"));
        }
    }

    @Test void cancelledScreenshotDoesNotCallReviewer() throws Exception {
        var started = new java.util.concurrent.CountDownLatch(1);
        var interrupted = new java.util.concurrent.CountDownLatch(1);
        doAnswer(call -> {
            started.countDown();
            try { new java.util.concurrent.CountDownLatch(1).await(); }
            catch (InterruptedException error) { interrupted.countDown(); throw error; }
            return null;
        }).when(screenshots).capture(anyString(), any(), anyInt(), anyInt());
        var subscription = workflow.run(9200, type, "shop", prompt -> Flux.just("code")).subscribe();
        assertTrue(started.await(5, java.util.concurrent.TimeUnit.SECONDS));
        subscription.dispose();
        assertTrue(interrupted.await(5, java.util.concurrent.TimeUnit.SECONDS));
        verify(reviewer, never()).review(any(), anyList());
        verify(screenshots, times(1)).capture(anyString(), any(), anyInt(), anyInt());
    }
}
