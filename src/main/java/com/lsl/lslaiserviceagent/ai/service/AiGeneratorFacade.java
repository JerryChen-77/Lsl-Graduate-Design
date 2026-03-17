package com.lsl.lslaiserviceagent.ai.service;

import cn.hutool.json.JSONUtil;
import com.lsl.lslaiserviceagent.ai.factory.AiGeneratorServiceFactory;
import com.lsl.lslaiserviceagent.ai.model.message.AiResponseMessage;
import com.lsl.lslaiserviceagent.ai.model.message.ReasoningMessage;
import com.lsl.lslaiserviceagent.ai.model.message.ToolExecutedMessage;
import com.lsl.lslaiserviceagent.ai.model.message.ToolRequestMessage;
import com.lsl.lslaiserviceagent.exception.BusinessException;
import com.lsl.lslaiserviceagent.exception.ErrorCode;
import com.lsl.lslaiserviceagent.model.enums.AiGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.el.parser.Token;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Service
@Slf4j
public class AiGeneratorFacade {

    private static final String LOCAL_IP = "127.0.0.1";
    @Resource
    private AiGeneratorServiceFactory aiGeneratorServiceFactory;

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage   用户提示词
     * @param aiGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public String generateAiAnswer(long chatId, String userMessage, AiGenTypeEnum aiGenTypeEnum) {
        if (aiGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        AiGeneratorService aiGeneratorService = aiGeneratorServiceFactory.getAiCodeGeneratorService(chatId,LOCAL_IP);
        return switch (aiGenTypeEnum) {
            case COMMON_CONVERSATION -> aiGeneratorService.generateCommonAnswer(userMessage);
            default -> {
                String errorMessage = "不支持的生成类型：" + aiGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    public Flux<String> generateAiAnswerStream(long chatId,String userMessage, AiGenTypeEnum aiGenTypeEnum,String ip) {
        if (aiGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        AiGeneratorService aiGeneratorService = aiGeneratorServiceFactory.getAiCodeGeneratorService(chatId,ip);
        return switch (aiGenTypeEnum) {
            case COMMON_CONVERSATION -> processAiAnswerStream(chatId,aiGeneratorService,userMessage);
            default -> {
                String errorMessage = "不支持的生成类型：" + aiGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    public Flux<String> processAiAnswerStream(Long chatId,AiGeneratorService aiGeneratorService,String userMessage) {
        TokenStream resTokens =  aiGeneratorService.generateCommonAnswerStream(chatId, userMessage);
        return processTokenStream(resTokens);
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialThinking((PartialThinking thinking) ->{
                        ReasoningMessage reasoningMessage = new ReasoningMessage(thinking);
                        sink.next(JSONUtil.toJsonStr(reasoningMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }
}
