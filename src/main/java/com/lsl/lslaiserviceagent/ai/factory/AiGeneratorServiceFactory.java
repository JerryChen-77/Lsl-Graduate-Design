package com.lsl.lslaiserviceagent.ai.factory;

import cn.hutool.extra.spring.SpringUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lsl.lslaiserviceagent.ai.guard.PromptSafetyInputGuardrail;
import com.lsl.lslaiserviceagent.ai.model.constants.AiConstants;
import com.lsl.lslaiserviceagent.ai.service.AiGeneratorService;
import com.lsl.lslaiserviceagent.facade.ChatHistroyFacade;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;

@Slf4j
@Configuration
public class AiGeneratorServiceFactory {

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    @Lazy
    private ChatHistroyFacade chatHistroyFacade;

    private final String SERVICE_CACHE_PREFIX = "lsl-agent-aiservice-cache:";


    public AiGeneratorService getAiCodeGeneratorService(long chatId) {
        // 通过缓存获取
        return serviceCache.get(buildKey(chatId), this::createAiCodeGeneratorService);
    }

    public AiGeneratorService createAiCodeGeneratorService(String key) {
        String[] split = key.split(":");
        Long chatId = Long.valueOf(split[1]);
        // 直接创建
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .id(chatId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(60)
                .build();
        chatHistroyFacade.loadChatHistoryToMemory(chatId,memory,60);
        StreamingChatModel streamingChatModelPrototype
                = SpringUtil.getBean(AiConstants.CHAT_AI_BEAN_NAME
                , StreamingChatModel.class);
        return AiServices.builder(AiGeneratorService.class)
                .streamingChatModel(streamingChatModelPrototype)
                .chatMemoryProvider(memoryId -> memory)
                //TODO 添加查询根据IP查询地址工具
//                .tools(new RelationDbTools(datasourceconfigFacade, chatHistroyFacade),
//                        new RedisTools(chatHistroyFacade,redisOperationService,datasourceconfigService))
                .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                        toolExecutionRequest,"There is no tools called"+ toolExecutionRequest.name()
                ))
                .maxSequentialToolsInvocations(20)
                //添加输入护轨
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
            })
            .build();

    private String buildKey(Long chatId){
        return SERVICE_CACHE_PREFIX+chatId;
    }
}
