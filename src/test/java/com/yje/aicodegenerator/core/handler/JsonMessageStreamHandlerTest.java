package com.yje.aicodegenerator.core.handler;

import cn.hutool.json.JSONUtil;
import com.yje.aicodegenerator.ai.model.message.ToolExecutedMessage;
import com.yje.aicodegenerator.ai.tools.FileReadTool;
import com.yje.aicodegenerator.ai.tools.ToolManager;
import com.yje.aicodegenerator.model.entity.User;
import com.yje.aicodegenerator.model.enums.ChatHistoryMessageTypeEnum;
import com.yje.aicodegenerator.service.ChatHistoryService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecution;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JsonMessageStreamHandlerTest {

    @Test
    void malformedArgumentsDoNotAbortExecutedToolMessages() {
        verifyResult("not-json", true, "file contents", "file contents");
        verifyResult("[]", true, "file contents", "file contents");
    }

    @Test
    void missingParametersAndUnknownToolsDoNotAbortStream() {
        verifyResult(null, true, "file contents", "读取文件");
        verifyResult("{}", false, "file contents", "file contents");
        verifyResult("bad", true, "", "工具未返回内容");
    }

    @Test
    void validArgumentsKeepExistingDisplayFormat() {
        verifyResult("{\"relativeFilePath\":\"package.json\"}", true,
                "file contents", "package.json");
    }

    private void verifyResult(String arguments, boolean knownTool, String result, String expected) {
        JsonMessageStreamHandler handler = new JsonMessageStreamHandler();
        ToolManager tools = mock(ToolManager.class);
        if (knownTool) {
            when(tools.getTool("readFile")).thenReturn(new FileReadTool());
        }
        ReflectionTestUtils.setField(handler, "toolManager", tools);
        ChatHistoryService history = mock(ChatHistoryService.class);
        User user = new User();
        user.setId(2L);
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("call-1").name("readFile").arguments(arguments).build();
        ToolExecutedMessage message = new ToolExecutedMessage(ToolExecution.builder()
                .request(request).result(result).build());

        var chunks = handler.handle(Flux.just(JSONUtil.toJsonStr(message)), history, 1L, user)
                .collectList().block();
        assertNotNull(chunks);
        assertEquals(1, chunks.size());
        assertTrue(chunks.getFirst().contains(expected));
        verify(history).addChatMessage(eq(1L), contains(expected),
                eq(ChatHistoryMessageTypeEnum.AI.getValue()), eq(2L));
    }
}
