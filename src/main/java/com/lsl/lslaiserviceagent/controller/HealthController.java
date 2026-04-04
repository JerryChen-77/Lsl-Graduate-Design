package com.lsl.lslaiserviceagent.controller;

import com.lsl.lslaiserviceagent.aop.anno.IpAddress;
import com.lsl.lslaiserviceagent.model.request.IpRequest;
import com.lsl.lslaiserviceagent.model.response.AmapResponse;
import com.lsl.lslaiserviceagent.utils.IpUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/health")
public class HealthController {


    @Resource
    private IpUtils ipUtils;

    @PostMapping("/")
    @IpAddress
    public String healthCheck(@RequestBody HealthRequest request) {
        return "ok";
    }


    @GetMapping("/ip/analyze")
    public AmapResponse ipAnalyze(HttpServletRequest request) {
        String ip = ipUtils.getClientIp(request);
        return ipUtils.getAddressByIp(ip);
    }
}
@Data
class HealthRequest extends IpRequest {
    private String name;
}
