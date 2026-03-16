package com.lsl.lslaiserviceagent.ai.model.constants;

public interface AiConstants {
    String FORMAT_SQL_TOOL_EXECUTE = """
                        {
                            "type":"[工具调用]",
                            "toolName":"%s"
                        }
                        ```sql
                        %s
                        ```
                        """;
    String FORMAT_NORMAL_TOOL_EXECUTE = """
                        {
                            "type":"[工具调用]",
                            "toolName":"%s"
                        }
                        """;
    String FORMAT_GET_TABLE_TOOL = """
                        {
                            "type":"[工具调用]",
                            "toolName":"%s"
                        }
                        成功获取数据表 %s
                        """;

    String FORMAT_ERROR_TOOL_EXECUTE_MSG = "error with toolName: %s there is no such tool";

    String ROUTING_AI_BEAN_NAME = "routingChatModelPrototype";

    String CHAT_AI_BEAN_NAME = "streamingChatModelPrototype";

    String REASONING_AI_BEAN_NAME = "reasoningStreamingChatModelPrototype";
}
