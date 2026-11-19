package com.yjx.yupicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yjx.yupicturebackend.exception.BusinessException;
import com.yjx.yupicturebackend.exception.ErrorCode;
import com.yjx.yupicturebackend.exception.ThrowUtils;
import com.yjx.yupicturebackend.mapper.SpaceMapper;
import com.yjx.yupicturebackend.model.dto.space.analyze.*;
import com.yjx.yupicturebackend.model.entity.Picture;
import com.yjx.yupicturebackend.model.entity.Space;
import com.yjx.yupicturebackend.model.entity.User;
import com.yjx.yupicturebackend.model.vo.space.analyze.*;
import com.yjx.yupicturebackend.service.PictureService;
import com.yjx.yupicturebackend.service.SpaceAnalyzeService;
import com.yjx.yupicturebackend.service.SpaceService;
import com.yjx.yupicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author yu
 * @createDate 2026-11-16 00:52:09
 */
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
                                                          User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 全空间或公共空间需要，需要从 Picture 表查询
        if (spaceUsageAnalyzeRequest.isQueryPublic() || spaceUsageAnalyzeRequest.isQueryAll()) {
            // 权限校验，仅管理员可访问
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            // 统计图库的使用情况
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            // 补充查询范围
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize = pictureObjList.stream()
                    .mapToLong(obj -> (Long) obj)
                    .sum();
            long usedCount = pictureObjList.size();
            // 封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        } else {
            // 私有空间直接从 Space 表查询
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 权限校验
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            // 封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            // 计算比例
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeResponse;
        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 权限校验
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        // 使用 Mybatis-plus 的分组查询功能，按 category 分组统计数量和大小
        queryWrapper.select("category AS category", "COUNT(*) AS count", "SUM(picSize) AS) totalSize")
                .groupBy("category");
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = (String) result.get("category");
                    Long count = (Long) result.get("count");
                    Long totalSize = (Long) result.get("totalSize");
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .toList();
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 权限校验
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        queryWrapper.select("tags");
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();
        // 解析 tagsJsonList，统计每个标签的数量
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        // 封装返回结果，按照数量降序排序
        return tagCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 权限校验
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);
        queryWrapper.select("picSize");
        List<Long> picSizeList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(obj -> (Long) obj)
                .toList();
        // 定义分段范围，注意使用有序的 map
        Map<String, Long> sizeMap = new LinkedHashMap<>();
        sizeMap.put("<100KB", picSizeList.stream().filter(size -> size < 100 * 1024).count());
        sizeMap.put("100KB-500KB", picSizeList.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeMap.put("500KB-1MB", picSizeList.stream().filter(size -> size >= 500 * 1024 && size < 1024 * 1024).count());
        sizeMap.put(">1MB", picSizeList.stream().filter(size -> size >= 1024 * 1024).count());
        return sizeMap.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 权限校验
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
        Long userId = loginUser.getId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        // 分析维度：每日、每周、每月上传数量
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case  "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime, 1) AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }
        // 分组并排序统计
        queryWrapper.groupBy("period").orderByAsc("period");
        // 查询并封装结果
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String period = (String) result.get("period");
                    Long count = (Long) result.get("count");
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .toList();
    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 权限校验,仅管理员可访问
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权查看空间排行");
        // 构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT" + spaceRankAnalyzeRequest.getTopN());
        return spaceService.list(queryWrapper);
    }

    /**
     * 检查空间分析权限
     *
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        // 检查是否有权限分析空间
        if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
            // 如果查询所有空间或公共空间，检查用户是否为管理员
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权限分析公共空间");
        } else {
            // 私有空间权限校验
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    /**
     * 填充分析查询条件
     *
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        // 全部空间
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        }
        // 公共空间
        if (spaceAnalyzeRequest.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
            return;
        }
        // 私有空间
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }

}




