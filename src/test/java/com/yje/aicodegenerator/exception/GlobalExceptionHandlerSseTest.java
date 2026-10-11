package com.yje.aicodegenerator.exception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerSseTest {

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void writesSseErrorWhenOutputStreamWasAlreadySelected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/app/chat/gen/code");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.getOutputStream();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

        assertNull(new GlobalExceptionHandler().businessExceptionHandler(
                new BusinessException(ErrorCode.NO_AUTH_ERROR, "not allowed")));
        String body = response.getContentAsString();
        assertTrue(body.contains("event: business-error"));
        assertTrue(body.contains("not allowed"));
        assertTrue(body.contains("event: done"));
    }

    @Test
    void doesNotAppendToCommittedResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "text/event-stream");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.getOutputStream().write("existing".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        response.flushBuffer();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

        assertNull(new GlobalExceptionHandler().runtimeExceptionHandler(new RuntimeException("failure")));
        assertEquals("existing", response.getContentAsString());
    }
}
