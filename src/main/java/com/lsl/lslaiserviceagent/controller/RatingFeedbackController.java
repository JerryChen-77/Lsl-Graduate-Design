package com.lsl.lslaiserviceagent.controller;

import com.lsl.lslaiserviceagent.aop.anno.IpAddress;
import com.lsl.lslaiserviceagent.common.BaseResponse;
import com.lsl.lslaiserviceagent.model.entity.User;
import com.lsl.lslaiserviceagent.model.request.rating.RateRequest;
import com.lsl.lslaiserviceagent.service.UserService;
import com.lsl.lslaiserviceagent.utils.common.ResultUtils;
import com.mybatisflex.core.paginate.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import com.lsl.lslaiserviceagent.model.entity.RatingFeedback;
import com.lsl.lslaiserviceagent.service.RatingFeedbackService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 评分反馈表 控制层。
 *
 * @author SiLin li
 */
@RestController
@RequestMapping("/ratingFeedback")
public class RatingFeedbackController {

    @Autowired
    private RatingFeedbackService ratingFeedbackService;

    @Autowired
    private UserService userService;

    @PostMapping("/rate")
    @IpAddress
    public BaseResponse<Boolean> rateAnswer(@RequestBody RateRequest request, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(ratingFeedbackService.rateAnswer(request, loginUser.getId()));
    }
}
