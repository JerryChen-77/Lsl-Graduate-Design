package com.lsl.lslaiserviceagent.controller;

import com.lsl.lslaiserviceagent.aop.anno.IpAddress;
import com.lsl.lslaiserviceagent.model.request.IpRequest;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/health")
public class HealthController {


    @PostMapping("/")
    @IpAddress
    public String healthCheck(@RequestBody HealthRequest request) {
        return "ok";
    }
}
@Data
class HealthRequest extends IpRequest {
    private String name;
}
