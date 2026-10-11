package com.yje.aicodegenerator.controller;

import com.yje.aicodegenerator.exception.BusinessException;
import com.yje.aicodegenerator.exception.ErrorCode;
import com.yje.aicodegenerator.model.entity.User;
import com.yje.aicodegenerator.service.AppService;
import com.yje.aicodegenerator.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppControllerSseTest {
    @Test
    void heartbeatAllowsQuietStreamCancellationWithoutAnotherGenerationSubscription() {
        AppController controller = new AppController();
        AppService appService = mock(AppService.class);
        UserService userService = mock(UserService.class);
        ReflectionTestUtils.setField(controller, "appService", appService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        var request = new MockHttpServletRequest();
        User user = new User();
        var cancelled = new java.util.concurrent.atomic.AtomicBoolean();
        var subscriptions = new java.util.concurrent.atomic.AtomicInteger();
        when(userService.getLoginUser(request)).thenReturn(user);
        when(appService.chatToGenCode(3L, "shop", user)).thenReturn(Flux.<String>never()
                .doOnSubscribe(s -> subscriptions.incrementAndGet()).doOnCancel(() -> cancelled.set(true)));
        var heartbeat = controller.chatToGenCode(3L, "shop", request).blockFirst(java.time.Duration.ofSeconds(5));
        assertEquals("keepalive", heartbeat.comment());
        assertTrue(cancelled.get());
        assertEquals(1, subscriptions.get());
    }
    @Test
    void designStagesRemainSeparateAndDoneIsLast() {
        AppController controller = new AppController();
        AppService appService = mock(AppService.class);
        UserService userService = mock(UserService.class);
        ReflectionTestUtils.setField(controller, "appService", appService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        var request = new MockHttpServletRequest();
        User user = new User();
        when(userService.getLoginUser(request)).thenReturn(user);
        String prefix = com.yje.aicodegenerator.design.DesignWorkflow.STATUS_PREFIX;
        when(appService.chatToGenCode(2L, "shop", user)).thenReturn(Flux.just(
                prefix + "{\"stage\":\"planning\",\"message\":\"plan\"}", "code",
                prefix + "{\"stage\":\"skipped\",\"message\":\"unavailable\"}"));
        var events = controller.chatToGenCode(2L, "shop", request).collectList().block();
        assertEquals("design-status", events.get(0).event());
        assertEquals("{\"stage\":\"planning\",\"message\":\"plan\"}", events.get(0).data());
        assertNull(events.get(1).event());
        assertEquals("design-status", events.get(2).event());
        assertEquals("done", events.get(3).event());
        verify(appService, times(1)).chatToGenCode(2L, "shop", user);
    }

    @Test
    void emitsErrorAndDoneAfterPartialOutput() {
        AppController controller = new AppController();
        AppService appService = mock(AppService.class);
        UserService userService = mock(UserService.class);
        ReflectionTestUtils.setField(controller, "appService", appService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        User user = new User();
        when(userService.getLoginUser(request)).thenReturn(user);
        when(appService.chatToGenCode(1L, "hello", user)).thenReturn(
                Flux.concat(Flux.just("partial"), Flux.error(
                        new BusinessException(ErrorCode.PARAMS_ERROR, "invalid tool input"))));

        var events = controller.chatToGenCode(1L, "hello", request).collectList().block();
        assertNotNull(events);
        assertEquals(3, events.size());
        assertTrue(events.get(0).data().contains("partial"));
        assertEquals("business-error", events.get(1).event());
        assertTrue(events.get(1).data().contains("invalid tool input"));
        assertEquals("done", events.get(2).event());
    }
}
