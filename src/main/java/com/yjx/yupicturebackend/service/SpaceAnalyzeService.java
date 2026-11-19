package com.yjx.yupicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yjx.yupicturebackend.model.dto.space.analyze.*;
import com.yjx.yupicturebackend.model.entity.Space;
import com.yjx.yupicturebackend.model.entity.User;
import com.yjx.yupicturebackend.model.vo.space.analyze.*;

import java.util.List;

/**
 * @author yu
 * @createDate 2026-11-16 00:52:09
 */
public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 获取空间使用情况分析
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    /**
     * 获取空间图片分类分析
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 获取空间图片标签分析
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 获取空间图片大小分析
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 获取用户上传行为分析
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 获取空间使用排行分析
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);

}
