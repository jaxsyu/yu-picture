package com.yjx.yupicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.yjx.yupicturebackend.manager.auth.SpaceUserAuthManager;
import com.yjx.yupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.yjx.yupicturebackend.model.entity.Picture;
import com.yjx.yupicturebackend.model.entity.Space;
import com.yjx.yupicturebackend.model.entity.User;
import com.yjx.yupicturebackend.model.enums.SpaceTypeEnum;
import com.yjx.yupicturebackend.service.PictureService;
import com.yjx.yupicturebackend.service.SpaceService;
import com.yjx.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (!(request instanceof ServletServerHttpRequest)) {
            return false;
        }
        HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        String pictureId = httpServletRequest.getParameter("pictureId");
        if (StrUtil.isBlank(pictureId)) {
            log.error("缺少图片参数，拒绝握手");
            return false;
        }
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (ObjUtil.isEmpty(loginUser)) {
            log.error("用户未登录，拒绝握手");
            return false;
        }
        // 检验用户是否有编辑当前图片的权限
        Picture picture = pictureService.getById(pictureId);
        if (ObjUtil.isEmpty(picture)) {
            log.error("图片不存在，拒绝握手");
            return false;
        }
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if (spaceId != null) {
            space = spaceService.getById(spaceId);
            if (ObjUtil.isEmpty(space)) {
                log.error("图片所在空间不存在，拒绝握手");
                return false;
            }
            if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                log.error("图片所在空间不是团队空间，拒绝握手");
                return false;
            }
        }
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
            log.error("用户没有编辑图片的权限，拒绝握手");
            return false;
        }
        // 设置图片 ID、用户信息等到 WebSocket 对话中
        attributes.put("user", loginUser);
        attributes.put("userId", loginUser.getId());
        attributes.put("pictureId", Long.valueOf(pictureId));// 记得转换为 Long 类型
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, @Nullable Exception exception) {

    }
}
