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
import com.yjx.yupicturebackend.manager.CosManager;
import com.yjx.yupicturebackend.manager.upload.FilePictureUpload;
import com.yjx.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.yjx.yupicturebackend.manager.upload.UrlPictureUpload;
import com.yjx.yupicturebackend.mapper.PictureMapper;
import com.yjx.yupicturebackend.model.dto.picture.*;
import com.yjx.yupicturebackend.model.entity.Picture;
import com.yjx.yupicturebackend.model.entity.User;
import com.yjx.yupicturebackend.model.enums.PictureReviewStatusEnum;
import com.yjx.yupicturebackend.model.vo.PictureVO;
import com.yjx.yupicturebackend.model.vo.UserVO;
import com.yjx.yupicturebackend.service.PictureService;
import com.yjx.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author yu
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2026-03-26 08:28:16
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private CosManager cosManager;

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新，校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人或管理员可更新图片
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        // 上传并解析图片
        // 按照用户 ID 划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        // 根据 inputSource 类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 数据库保存上传的图片信息
        Picture picture = new Picture();
        BeanUtil.copyProperties(uploadPictureResult, picture);
        // 设置图片名称
        if(pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())){
            picture.setName(pictureUploadRequest.getPicName());
        }
        picture.setUserId(loginUser.getId());
        // 补充审核信息
        this.fillReviewParams(picture, loginUser);
        // 如果 ID 不为空，表示更新，否则为新增
        if (pictureId != null) {
            // 是更新则补充图片 ID 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        // 是更新的话，也要清理Cos中的老图片
//        this.clearPictureFile(oldPicture);
        return PictureVO.objToVo(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从请求对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        Long userId = pictureQueryRequest.getUserId();
        String searchText = pictureQueryRequest.getSearchText();
        List<String> tags = pictureQueryRequest.getTags();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();

        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotNull(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotNull(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotNull(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotNull(picScale), "picScale", picScale);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);

        // 从多字段中查询（根据 name 和 introduction 查询）
        if (StrUtil.isNotBlank(searchText)) {
            // 拼接查询条件
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                            .or()
                            .like("introduction", searchText)
            );
        }

        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }

        // 排序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("Ascend"), sortField);

        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联创建用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(),
                picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, User> userIdUserMap = userService.listByIds(userIdSet).stream().
                collect(Collectors.toMap(User::getId, Function.identity()));
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserMap.containsKey(userId)) {
                user = userIdUserMap.get(userId);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 1024, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //是否重复审核
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        //数据库操作，更新审核状态
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        // 管理员自动过审
        if (userService.isAdmin(loginUser)) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，上传或编辑都要设置为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if(StrUtil.isBlank(namePrefix)){
            namePrefix = searchText;
        }
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多30条");
        // 抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if(ObjUtil.isNull(div)){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        // 遍历元素，依次上传图片
        int uploadCount = 0;
        for(Element imgElement : imgElementList){
            String fileUrl = imgElement.attr("src");
            if(StrUtil.isBlank(fileUrl)){
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            // 处理图片地址，防止出现转义或和对象存储冲突的问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if(questionMarkIndex > -1){
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 图片上传
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，ID = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("上传图片失败", e);
                continue;
            }
            if(uploadCount >= count){
                break;
            }
        }
        return uploadCount;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断图片是否被多条记录使用
        String url = oldPicture.getUrl();
        Long count = this.lambdaQuery()
                .eq(Picture::getUrl, url)
                .count();
        // 不止一条使用，不清理
        if (count > 1){
            return;
        }
        cosManager.deleteObject(url);
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if(StrUtil.isNotBlank(thumbnailUrl)){
            cosManager.deleteObject(thumbnailUrl);
        }
    }
}
