package com.lsl.lslaiserviceagent.ai.core;


import com.lsl.lslaiserviceagent.model.entity.User;
import com.lsl.lslaiserviceagent.model.enums.AiGenTypeEnum;
import com.lsl.lslaiserviceagent.service.ChatHistoryOriginalService;
import com.lsl.lslaiserviceagent.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 流处理器执行器
 * 根据代码生成类型创建合适的流处理器：
 * 1. 传统的 Flux<String> 流（HTML、MULTI_FILE） -> SimpleTextStreamHandler
 * 2. TokenStream 格式的复杂流（VUE_PROJECT） -> JsonMessageStreamHandler
 */
@Slf4j
@Component
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    /**
     * 创建流处理器并处理聊天历史记录
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param chatId              对话ID
     * @param loginUser          登录用户
     * @param typeEnum        AI对话类型
     * @return 处理后的流
     */
    public Flux<String> doExecute(Flux<String> originFlux,
                                  ChatHistoryService chatHistoryService,
                                  ChatHistoryOriginalService chatHistoryOriginalService,
                                  long chatId, User loginUser, AiGenTypeEnum typeEnum) {
        return switch (typeEnum) {
            case COMMON_CONVERSATION-> // 使用注入的组件实例
                    jsonMessageStreamHandler.handle(originFlux, chatHistoryService,chatHistoryOriginalService, chatId, loginUser);
            default -> Flux.empty();
        };
    }
}
