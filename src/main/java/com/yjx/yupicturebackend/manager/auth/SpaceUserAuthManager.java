package com.yjx.yupicturebackend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;
import com.yjx.yupicturebackend.manager.auth.model.SpaceUserAuthConfig;
import com.yjx.yupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.yjx.yupicturebackend.manager.auth.model.SpaceUserRole;
import com.yjx.yupicturebackend.model.entity.Space;
import com.yjx.yupicturebackend.model.entity.SpaceUser;
import com.yjx.yupicturebackend.model.entity.User;
import com.yjx.yupicturebackend.model.enums.SpaceRoleEnum;
import com.yjx.yupicturebackend.model.enums.SpaceTypeEnum;
import com.yjx.yupicturebackend.service.SpaceUserService;
import com.yjx.yupicturebackend.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SpaceUserAuthManager {

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

    // 加载配置文件到对象
    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    // 根据角色获取权限列表
    public List<String> getPermissionsByRole(String spaceUserRole){
        if(spaceUserRole == null){
            return new ArrayList<>();
        }
        // 找到匹配的角色
        return SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(role -> role.getKey().equals(spaceUserRole))
                .findFirst()
                .map(SpaceUserRole::getPermissions)
                .orElse(new ArrayList<>());
    }

    /**
     * 获取权限列表
     *
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        // 根据空间获取对应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询 SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }
}
