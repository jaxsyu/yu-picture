package com.yjx.yupicturebackend.aop;


import com.yjx.yupicturebackend.annotation.AuthCheck;
import com.yjx.yupicturebackend.constant.UserConstant;
import com.yjx.yupicturebackend.exception.ErrorCode;
import com.yjx.yupicturebackend.exception.ThrowUtils;
import com.yjx.yupicturebackend.model.entity.User;
import com.yjx.yupicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        //获取当前登录用户的角色
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getLoginUser(request);
        String userRole = loginUser.getUserRole();
        //获取被拦截方法所需的mustRole
        String mustRole = authCheck.mustRole();
        //开始校验权限
        //不需要权限，则放行，执行原本被拦截的方法
        if (mustRole == null) {
            joinPoint.proceed();
        }
        //以下为需要权限的情况
        //用户无权限，拒绝
        ThrowUtils.throwIf(userRole == null, ErrorCode.NO_AUTH_ERROR);
        //需要管理员权限，但用户不是管理员
        ThrowUtils.throwIf(UserConstant.ADMIN_ROLE.equals(mustRole) && !UserConstant.ADMIN_ROLE.equals(userRole),
                ErrorCode.NO_AUTH_ERROR);
        return joinPoint.proceed();
    }
}
