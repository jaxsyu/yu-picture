package com.yjx.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yjx.yupicturebackend.exception.BusinessException;
import com.yjx.yupicturebackend.exception.ErrorCode;
import com.yjx.yupicturebackend.exception.ThrowUtils;
import com.yjx.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.yjx.yupicturebackend.model.dto.space.SpaceQueryRequest;
import com.yjx.yupicturebackend.model.entity.Space;
import com.yjx.yupicturebackend.model.entity.User;
import com.yjx.yupicturebackend.model.enums.SpaceLevelEnum;
import com.yjx.yupicturebackend.model.vo.SpaceVO;
import com.yjx.yupicturebackend.model.vo.UserVO;
import com.yjx.yupicturebackend.service.SpaceService;
import com.yjx.yupicturebackend.mapper.SpaceMapper;
import com.yjx.yupicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author yu
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2026-11-16 00:52:09
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        Space space = new Space();
        BeanUtil.copyProperties(spaceAddRequest, space);
        // 设置默认值
        if(StrUtil.isBlank(spaceAddRequest.getSpaceName())){
            space.setSpaceName("默认空间");
        }
        if(spaceAddRequest.getSpaceLevel() == null){
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        // 填充数据
        this.fillSpaceBySpaceLevel(space);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 数据校验
        this.validSpace(space, true);
        // 权限校验
        if (SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建该级别空间");
        }
        // 针对用户上锁
        String lock = String.valueOf(userId).intern();
        synchronized (lock){
            Long newSpaceId = transactionTemplate.execute(status -> {
                // 查询用户是否已拥有空间
                boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每位用户仅可拥有一个私有空间");
                // 创建空间,写入数据库
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                return space.getId();
            });
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 创建时校验
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            ThrowUtils.throwIf(spaceLevel == null, ErrorCode.PARAMS_ERROR, "空间级别不能为空");
        }
        // 修改时
        ThrowUtils.throwIf(StrUtil.isNotBlank(spaceName) && spaceName.length() > 30,
                ErrorCode.PARAMS_ERROR, "空间名称过长");
        ThrowUtils.throwIf(spaceLevel != null && spaceLevelEnum == null,
                ErrorCode.PARAMS_ERROR, "空间级别不存在");
    }

    @Override
    public SpaceVO getSpaceVO(Space space) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联创建用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从请求对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotNull(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("Ascend"), sortField);
        return queryWrapper;
    }


    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(),
                spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, User> userIdUserMap = userService.listByIds(userIdSet).stream().
                collect(Collectors.toMap(User::getId, Function.identity()));
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserMap.containsKey(userId)) {
                user = userIdUserMap.get(userId);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if(spaceLevelEnum != null){
            long maxSize = spaceLevelEnum.getMaxSize();
            if(space.getMaxSize() == null){
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if(space.getMaxCount() == null){
                space.setMaxCount(maxCount);
            }
        }
    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        // 仅本人或管理员可访问
        ThrowUtils.throwIf(!userService.isAdmin(loginUser) && !space.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权限访问该空间");
    }

}




