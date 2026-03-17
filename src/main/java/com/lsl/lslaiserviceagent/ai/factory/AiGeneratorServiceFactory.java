package com.lsl.lslaiserviceagent.ai.factory;

import cn.hutool.extra.spring.SpringUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lsl.lslaiserviceagent.ai.guard.PromptSafetyInputGuardrail;
import com.lsl.lslaiserviceagent.ai.model.constants.AiConstants;
import com.lsl.lslaiserviceagent.ai.service.AiGeneratorService;
import com.lsl.lslaiserviceagent.ai.tool.IpTools;
import com.lsl.lslaiserviceagent.facade.ChatHistroyFacade;
import com.lsl.lslaiserviceagent.utils.IpUtils;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;
import java.util.List;

@Slf4j
@Configuration
public class AiGeneratorServiceFactory {

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    @Lazy
    private ChatHistroyFacade chatHistroyFacade;

    private static final String SERVICE_CACHE_PREFIX = "lsl-agent-aiservice-cache";

    private static final String SEPERATOR = ":";

    private static final String RAG_DIRECTORY_PATH = "src\\main\\resources\\rag_documents";

    public AiGeneratorService getAiCodeGeneratorService(long chatId,String ip) {
        // 通过缓存获取
        return serviceCache.get(buildKey(chatId,ip), this::createAiCodeGeneratorService);
    }

    public AiGeneratorService createAiCodeGeneratorService(String key) {
        String[] split = key.split(":");
        Long chatId = Long.valueOf(split[1]);
        String ip = split[2];
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

        // 1. 加载文档

        List<Document> documents = FileSystemDocumentLoader.loadDocuments(RAG_DIRECTORY_PATH);

        // 2. 创建内存向量存储，并使用内置ingestor处理文档（一切默认！）
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor.ingest(documents, embeddingStore); // 魔法在这里

        return AiServices.builder(AiGeneratorService.class)
                .streamingChatModel(streamingChatModelPrototype)
                .chatMemoryProvider(memoryId -> memory)
                //TODO 添加查询根据IP查询地址工具
                .tools(new IpTools(new IpUtils(),ip))
                .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                        toolExecutionRequest,"There is no tools called"+ toolExecutionRequest.name()
                ))
                .maxSequentialToolsInvocations(20)
                //添加输入护轨
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore))
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

    private String buildKey(Long chatId,String ip){
        return SERVICE_CACHE_PREFIX+SEPERATOR+chatId+SEPERATOR+ip;
    }
}
