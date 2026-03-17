package com.lsl.lslaiserviceagent.ai.tool;

import com.lsl.lslaiserviceagent.model.response.AmapResponse;
import com.lsl.lslaiserviceagent.utils.IpUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IpTools {

    private final IpUtils ipUtils;

    private final String ip;
    public IpTools(IpUtils ipUtils,String ip){
        this.ipUtils = ipUtils;
        this.ip = ip;
    }

    @Tool("地址查询工具")
    public AmapResponse IP_ANALYZE(@ToolMemoryId Long chatId){
        return ipUtils.getAddressByIp(ip);
    }
}
