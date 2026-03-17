package com.lsl.lslaiserviceagent.controller;


import cn.hutool.json.JSONUtil;
import com.lsl.lslaiserviceagent.aop.anno.AuthCheck;
import com.lsl.lslaiserviceagent.common.BaseResponse;
import com.lsl.lslaiserviceagent.common.DeleteRequest;
import com.lsl.lslaiserviceagent.constant.UserConstant;
import com.lsl.lslaiserviceagent.exception.BusinessException;
import com.lsl.lslaiserviceagent.exception.ErrorCode;
import com.lsl.lslaiserviceagent.facade.ChatHistroyFacade;
import com.lsl.lslaiserviceagent.model.entity.ChatHistory;
import com.lsl.lslaiserviceagent.model.entity.ChatTopic;
import com.lsl.lslaiserviceagent.model.entity.User;
import com.lsl.lslaiserviceagent.model.request.chathistory.ChatHistoryQueryRequest;
import com.lsl.lslaiserviceagent.service.ChatTopicService;
import com.lsl.lslaiserviceagent.service.UserService;
import com.lsl.lslaiserviceagent.utils.IpUtils;
import com.lsl.lslaiserviceagent.utils.ResultUtils;
import com.lsl.lslaiserviceagent.utils.ThrowUtils;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 对话历史 控制层。
 *
 * @author Jiaxuan Chen
 */
@RestController
@RequestMapping("/chat")
public class ChatHistoryController {

    @Resource
    private ChatHistroyFacade chatHistroyFacade;
    @Resource
    private UserService userService;

    @Autowired
    private ChatTopicService chatTopicService;

    @Resource
    private IpUtils ipUtils;
    /**
     * 删除指定对话历史记录（仅创建者与管理员可见）
     *
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<Boolean> deleteChatTopic(@RequestBody DeleteRequest deleteRequest){
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        chatHistroyFacade.deleteChatByChatId(deleteRequest.getId());
        return ResultUtils.success(true);
    }

    /**
     * 分页查询某个对话的对话历史（游标查询）
     *
     * @param chatId          对话ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return 对话历史分页
     */
    @GetMapping("/history/{chatId}")
    public BaseResponse<Page<ChatHistory>> listChatHistory(@PathVariable Long chatId,
                                                           @RequestParam(defaultValue = "10") int pageSize,
                                                           @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                           HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistroyFacade.listAppChatHistoryByPage(chatId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询所有对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        Page<ChatHistory> result = chatHistroyFacade.listAllChatHistoryByPageForAdmin(pageNum, pageSize, chatHistoryQueryRequest);
        return ResultUtils.success(result);
    }

    /**
     * 聊天生成代码（流式 SSE）
     *
     * @param message      用户消息
     * @param request      请求对象
     * @return 生成结果流
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestParam String message,
                                              @RequestParam Long chatId,
                                              @RequestParam Boolean initChat,
                                              HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(StringUtils.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (!initChat && chatId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "对话历史记录异常");
        }
        // 如果是首次交谈Chat，需要添加chatTopic
        if (initChat) {
            ChatTopic newTopic = ChatTopic.builder()
                    .userId(loginUser.getId())
                    .content(message)
                    .build();
            chatTopicService.save(newTopic);
            chatId = newTopic.getId();
        }
        String ip = ipUtils.getClientIp(request);
        // 调用服务生成代码（流式）
        Flux<String> contentFlux = chatHistroyFacade.chat(chatId, message, loginUser,ip);
        return contentFlux
                .map(chunk -> {
                    // 包装成json对象
                    Map<String, String> wrapper = Map.of("v", chunk);
                    String jsonStr = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonStr)
                            .build();
                })
                .concatWith(Mono.just(
                        ServerSentEvent.<String>builder()
                                .event("chatId")
                                .data(chatId.toString())
                                .build()
                ))
                .concatWith(Mono.just(
                        // 触发结束事件
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }
}

