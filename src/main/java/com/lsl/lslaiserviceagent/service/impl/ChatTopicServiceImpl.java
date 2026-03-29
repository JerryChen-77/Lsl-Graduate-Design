package com.lsl.lslaiserviceagent.service.impl;


import com.lsl.lslaiserviceagent.exception.ErrorCode;
import com.lsl.lslaiserviceagent.mapper.ChatTopicMapper;
import com.lsl.lslaiserviceagent.model.entity.ChatTopic;
import com.lsl.lslaiserviceagent.service.ChatTopicService;
import com.lsl.lslaiserviceagent.utils.common.ThrowUtils;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 对话主题表 服务层实现。
 *
 * @author Jiaxuan Chen
 */
@Service
@Slf4j
public class ChatTopicServiceImpl extends ServiceImpl<ChatTopicMapper, ChatTopic> implements ChatTopicService {

    @Override
    public boolean removeByChatId(Long chatId) {
        ThrowUtils.throwIf(chatId == null, ErrorCode.PARAMS_ERROR, "chatId不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", chatId);
        boolean removeResult = this.remove(queryWrapper);
        if (!removeResult) {
            long remain = this.count(queryWrapper);
            ThrowUtils.throwIf(remain > 0, ErrorCode.DB_OPERATION_ERROR, "对话topic清理失败");
        }
        return true;
    }
}
