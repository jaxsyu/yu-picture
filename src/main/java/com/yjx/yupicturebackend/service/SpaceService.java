package com.yjx.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yjx.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.yjx.yupicturebackend.model.dto.space.SpaceQueryRequest;
import com.yjx.yupicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yjx.yupicturebackend.model.entity.User;
import com.yjx.yupicturebackend.model.vo.SpaceVO;

/**
 * @author yu
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2026-11-16 00:52:09
 */
public interface SpaceService extends IService<Space> {

    /**
     * 创建空间
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 用于更新或修改空间时的校验空间方法
     *
     * @param space
     * @param add   判断是创建还是编辑
     */
    void validSpace(Space space, boolean add);

    /**
     * 获取空间查询条件
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 获取空间封装类并关联创建用户信息
     *
     * @param space
     * @return
     */
    SpaceVO getSpaceVO(Space space);

    /**
     * 分页获取空间封装并关联用户信息
     *
     * @param spacePage
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage);

    /**
     * 根据空间级别自动填充限额
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 空间权限校验
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);
}
