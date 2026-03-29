package com.lsl.lslaiserviceagent.ai.core;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import com.lsl.lslaiserviceagent.ai.model.constants.AiConstants;
import com.lsl.lslaiserviceagent.ai.model.message.*;
import com.lsl.lslaiserviceagent.model.entity.ChatHistoryOriginal;
import com.lsl.lslaiserviceagent.model.entity.User;
import com.lsl.lslaiserviceagent.model.enums.AIToolsEnum;
import com.lsl.lslaiserviceagent.model.enums.ChatHistoryMessageTypeEnum;
import com.lsl.lslaiserviceagent.model.request.chathistory.ChatHistorySaveRequest;
import com.lsl.lslaiserviceagent.service.ChatHistoryOriginalService;
import com.lsl.lslaiserviceagent.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON 消息流处理器
 * 处理复杂流式响应，包含工具调用信息
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    /**
     * 处理 TokenStream
     * 解析 JSON 消息并重组为完整的响应格式
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param chatId             对话ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               ChatHistoryOriginalService chatHistoryOriginalService,
                               long chatId, User loginUser, long fatherId) {
        // 收集数据用于前端展示
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 收集用于恢复对话记忆的数据
        StringBuilder aiResponseStringBuilder = new StringBuilder();
        // 每个 Flux 流可能包含多条工具调用和 AI_RESPONSE 响应信息，统一收集之后批量入库
        List<ChatHistoryOriginal> originalChatHistoryList = new ArrayList<>();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                .map(chunk -> {
                    // 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder,
                            aiResponseStringBuilder,
                            originalChatHistoryList,
                            seenToolIds);
                })
                .filter(StrUtil::isNotEmpty) // 过滤空字串
                .doOnComplete(() -> {
                    // 工具调用信息入库
                    if (!originalChatHistoryList.isEmpty()) {
                        // 完善 ChatHistoryOriginal 信息
                        originalChatHistoryList.forEach(chatHistory -> {
                            chatHistory.setChatId(chatId);
                            chatHistory.setUserId(loginUser.getId());
                        });
                        // 批量入库
                        chatHistoryOriginalService.addOriginalChatMessageBatch(originalChatHistoryList);
                    }

                    // 流式响应完成后，添加AI消息到对话历史
                    String aiResponse = chatHistoryStringBuilder.toString();
// 处理深度思考内容
//                    String mergedMessage = mergeMessages(aiResponse);
//                    List<String> thinkingMsg = reasoningThinkingExtractor.extractReasoningData(mergedMessage);
//                    if (!thinkingMsg.isEmpty()) {
//                        for (String thinkingContent : thinkingMsg) {
//                            chatHistoryOriginalService.addOriginalChatMessage(chatId,
//                                    thinkingContent,
//                                    ChatHistoryMessageTypeEnum.REANSONING_THINKING.getValue(),
//                                    loginUser.getId());
//                        }
//                    }
                    // Ai response 入库(两种情况：1. 没有进行工具调用。2. 工具调用结束之后 AI 一般还会有一句返回)
                    String aiResponseStr = aiResponseStringBuilder.toString();
                    chatHistoryOriginalService.addOriginalChatMessage(chatId, aiResponseStr, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());

                    // 完整前端展示ChatHistory入库
                    ChatHistorySaveRequest req = ChatHistorySaveRequest.builder()
                            .chatId(chatId)
                            .fatherId(fatherId)
                            .message(aiResponse)
                            .userId(loginUser.getId())
                            .messageType(ChatHistoryMessageTypeEnum.AI.getValue())
                            .build();
                    chatHistoryService.saveChatMessage(req);
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    ChatHistorySaveRequest req = ChatHistorySaveRequest.builder()
                            .chatId(chatId)
                            .fatherId(fatherId)
                            .message(errorMessage)
                            .userId(loginUser.getId())
                            .messageType(ChatHistoryMessageTypeEnum.AI.getValue())
                            .build();
                    chatHistoryService.saveChatMessage(req);
                });
    }

    private String mergeMessages(String input) {
        StringBuilder result = new StringBuilder();

        // 正则表达式匹配 {"data":"...","type":"reasoning_message"} 格式
        Pattern reasoningPattern = Pattern.compile("\\{\"data\":\"(.*?)\",\"type\":\"reasoning_message\"\\}");
        Matcher matcher = reasoningPattern.matcher(input);

        int lastEnd = 0;
        StringBuilder currentReasoning = new StringBuilder();
        String currentType = null;

        while (matcher.find()) {
            int start = matcher.start();
            String text = matcher.group(1);
            String type = "reasoning_message"; // 因为是匹配 reasoning_message 类型

            // 检查当前位置是否与上一个匹配连续
            if (start != lastEnd) {
                // 不连续，说明中间有其他内容
                // 先处理已合并的 reasoning_message
                if (currentReasoning.length() > 0) {
                    result.append(String.format("{\"data\":\"%s\",\"type\":\"%s\"}",
                            currentReasoning.toString(), currentType));
                    currentReasoning.setLength(0);
                }

                // 添加中间的非 reasoning_message 内容
                String between = input.substring(lastEnd, start);
                result.append(between);
            }

            // 如果是 reasoning_message 且类型相同，则合并
            if (type.equals(currentType)) {
                currentReasoning.append(text);
            } else {
                // 类型变化，先保存之前的合并结果
                if (currentReasoning.length() > 0) {
                    result.append(String.format("{\"data\":\"%s\",\"type\":\"%s\"}",
                            currentReasoning.toString(), currentType));
                }
                currentReasoning.setLength(0);
                currentReasoning.append(text);
                currentType = type;
            }

            lastEnd = matcher.end();
        }

        // 处理最后一段 reasoning_message
        if (currentReasoning.length() > 0) {
            result.append(String.format("{\"data\":\"%s\",\"type\":\"%s\"}",
                    currentReasoning.toString(), currentType));
        }

        // 添加剩余的字符串（如果有）
        if (lastEnd < input.length()) {
            result.append(input.substring(lastEnd));
        }

        return result.toString();
    }
    /**
     * 解析并收集 TokenStream 数据
     */
    private String handleJsonMessageChunk(String chunk,
                                          StringBuilder chatHistoryStringBuilder,
                                          StringBuilder aiResponseStringBuilder,
                                          List<ChatHistoryOriginal> originalChatHistoryList,
                                          Set<String> seenToolIds) {
        // 解析 JSON
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                // 直接拼接响应
                chatHistoryStringBuilder.append(data);
                // 对于 AI 响应内容，与展示数据处理逻辑相同
                aiResponseStringBuilder.append(data);
                return data;
            }
            case REASONING_MESSAGE -> {
                chatHistoryStringBuilder.append(chunk);
                return chunk;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                String toolName = toolRequestMessage.getName();
                // 检查是否是第一次看到这个工具 ID
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    // 第一次调用这个工具，记录 ID 并完整返回工具信息
                    seenToolIds.add(toolId);
                    log.info("\n\n[选择工具] "+toolName+"\n\n");
                    return "";
                } else {
                    // 不是第一次调用这个工具，直接返回空
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                processToolExecutionMessage(aiResponseStringBuilder, chunk, originalChatHistoryList);

                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                String toolName = toolExecutedMessage.getName();
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());

                String output = formatToolExectuedOutput(jsonObject, toolName);
                chatHistoryStringBuilder.append(output);
                return output;
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }
    }

    private String formatToolExectuedOutput(JSONObject jsonObject, String toolName) {
        AIToolsEnum aiTools = AIToolsEnum.getEnumByValue(toolName);
        switch (aiTools) {
            case IP_ANALYZE_UTIL
                    -> {
                String result = String.format(AiConstants.FORMAT_NORMAL_TOOL_EXECUTE,aiTools.getDesc());
                return String.format("\n\n%s\n\n", result);
            }
            default -> {
                return String.format(AiConstants.FORMAT_ERROR_TOOL_EXECUTE_MSG,toolName);
            }
        }
    }

    /**
     * 解析处理工具调用相关信息
     * @param aiResponseStringBuilder
     * @param chunk
     * @param originalChatHistoryList
     */
    private void processToolExecutionMessage(StringBuilder aiResponseStringBuilder, String chunk, List<ChatHistoryOriginal> originalChatHistoryList) {
        // 解析 chunk
        ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
        // 构造工具调用请求对象(工具调用结果的数据就是从调用请求里拿的，所以直接在这里处理调用请求信息)
        String aiResponseStr = aiResponseStringBuilder.toString();
        ToolRequestMessage toolRequestMessage = new ToolRequestMessage();
        toolRequestMessage.setId(toolExecutedMessage.getId());
        toolRequestMessage.setName(toolExecutedMessage.getName());
        toolRequestMessage.setArguments(toolExecutedMessage.getArguments());
        toolRequestMessage.setText(aiResponseStr);
        // 转换成 JSON
        String toolRequestJsonStr = JSONUtil.toJsonStr(toolRequestMessage);
        // 构造 ChatHistory 存入列表
        ChatHistoryOriginal toolRequestHistory = ChatHistoryOriginal.builder()
                .message(toolRequestJsonStr)
                .messageType(ChatHistoryMessageTypeEnum.TOOL_EXECUTION_REQUEST.getValue())
                .build();
        originalChatHistoryList.add(toolRequestHistory);
        ChatHistoryOriginal toolResultHistory = ChatHistoryOriginal.builder()
                .message(chunk)
                .messageType(ChatHistoryMessageTypeEnum.TOOL_EXECUTION_RESULT.getValue())
                .build();
        originalChatHistoryList.add(toolResultHistory);
        // AI 响应内容暂时结束，置空 aiResponseStringBuilder
        aiResponseStringBuilder.setLength(0);
    }
}
