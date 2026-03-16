package com.lsl.lslaiserviceagent.aop;

import com.lsl.lslaiserviceagent.aop.anno.AuthCheck;
import com.lsl.lslaiserviceagent.aop.anno.IpAddress;
import com.lsl.lslaiserviceagent.exception.BusinessException;
import com.lsl.lslaiserviceagent.exception.ErrorCode;
import com.lsl.lslaiserviceagent.model.entity.User;
import com.lsl.lslaiserviceagent.model.enums.UserRoleEnum;
import com.lsl.lslaiserviceagent.model.request.IpRequest;
import com.lsl.lslaiserviceagent.utils.IpUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;

@Aspect
@Component
@Slf4j
public class IpAddressInterceptor {

    @Resource
    private IpUtils ipUtils;
    /**
     * 执行拦截
     *
     * @param joinPoint
     * @param ipAddress
     * @return
     */
    @Around("@annotation(ipAddress)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, IpAddress ipAddress) throws Throwable {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        String clientIp = ipUtils.getClientIp(request);
        if (StringUtils.isBlank(clientIp)) {
            log.error("未获取到客户端IP地址");
            throw new BusinessException(ErrorCode.NO_CLIENT_IP_ERROR);
        }
        log.info("客户端IP地址为：{}", clientIp);
        // 将IP地址注入到请求中
        ipUtils.getAddressByIp(clientIp);
        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof IpRequest) {
                // 使用ReflectionUtils查找并设置字段
                Field ipField = ReflectionUtils.findField(arg.getClass(), "ip");
                if (ipField != null) {
                    ReflectionUtils.makeAccessible(ipField);
                    ReflectionUtils.setField(ipField, arg, clientIp);
                }
                break;
            }
        }
        return joinPoint.proceed();
    }
}
