package com.lsl.lslaiserviceagent.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AiGeneratorService {

    /**
     * 生成 HTML 代码
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/answergen-service-hotline-prompt.txt")
    String generateCommonAnswer(String userMessage);

    @SystemMessage(fromResource = "prompt/answergen-service-hotline-prompt.txt")
    TokenStream generateCommonAnswerStream(@MemoryId Long chatId,@UserMessage String userMessage);
}
