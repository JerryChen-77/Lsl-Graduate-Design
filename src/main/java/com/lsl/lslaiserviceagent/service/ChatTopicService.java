package com.lsl.lslaiserviceagent.service;

import com.lsl.lslaiserviceagent.model.entity.ChatTopic;
import com.mybatisflex.core.service.IService;

/**
 * 对话主题表 服务层。
 *
 * @author Jiaxuan Chen
 */
public interface ChatTopicService extends IService<ChatTopic> {

    boolean removeByChatId(Long chatId);
}
