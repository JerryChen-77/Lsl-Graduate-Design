package com.lsl.lslaiserviceagent.facade;


import com.lsl.lslaiserviceagent.model.entity.ChatHistory;
import com.lsl.lslaiserviceagent.model.entity.User;
import com.lsl.lslaiserviceagent.model.request.chathistory.ChatHistoryQueryRequest;
import com.mybatisflex.core.paginate.Page;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

public interface ChatHistroyFacade {
    public boolean deleteChatByChatId(Long chatId);

    Page<ChatHistory> listAppChatHistoryByPage(Long chatId, int pageSize, LocalDateTime lastCreateTime, User loginUser);

    Page<ChatHistory> listAllChatHistoryByPageForAdmin(long pageNum, long pageSize, ChatHistoryQueryRequest chatHistoryQueryRequest);

    int loadChatHistoryToMemory(Long chatId, MessageWindowChatMemory chatMemory, int maxCount);


    Flux<String> chat(Long chatId,String message,User loginUser,String ip);
}
