package com.yjx.yupicturebackend.controller;


import com.yjx.yupicturebackend.common.BaseResponse;
import com.yjx.yupicturebackend.common.ResultUtils;
import com.yjx.yupicturebackend.exception.ErrorCode;
import com.yjx.yupicturebackend.exception.ThrowUtils;
import com.yjx.yupicturebackend.model.dto.space.analyze.*;
import com.yjx.yupicturebackend.model.entity.Space;
import com.yjx.yupicturebackend.model.entity.User;
import com.yjx.yupicturebackend.model.vo.space.analyze.*;
import com.yjx.yupicturebackend.service.SpaceAnalyzeService;
import com.yjx.yupicturebackend.service.SpaceService;
import com.yjx.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;


@Slf4j
@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    /**
     * 获取空间使用情况分析
     */
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(@RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
                                                                        HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        SpaceUsageAnalyzeResponse spaceUsageAnalyze = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceUsageAnalyze);
    }

    /**
     * 获取空间图片分类分析
     */
    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(@RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
                                                                                   HttpServletRequest request) {
            ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
            User loginUser = userService.getLoginUser(request);
            List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyzeResponseList = spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
            return ResultUtils.success(spaceCategoryAnalyzeResponseList);
    }

    /**
     * 获取空间图片标签分析
     */
    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(@RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
                                                                           HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceTagAnalyzeResponse> spaceTagAnalyzeResponseList = spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceTagAnalyzeResponseList);
    }

    /**
     * 获取空间图片大小分析
     */
    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyze(@RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest,
                                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceSizeAnalyzeResponse> spaceSizeAnalyzeResponseList = spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceSizeAnalyzeResponseList);
    }

    /**
     * 获取用户上传行为分析
     */
    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(@RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest,
                                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceUserAnalyzeResponse> spaceUserAnalyzeResponseList = spaceAnalyzeService.getSpaceUserAnalyze(spaceUserAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceUserAnalyzeResponseList);
    }

    /**
     * 获取空间使用排行分析
     */
    @PostMapping("/rank")
    public BaseResponse<List<Space>> getSpaceRankAnalyze(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<Space> spaceList = spaceAnalyzeService.getSpaceRankAnalyze(spaceRankAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceList);
    }

}
