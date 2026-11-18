package com.yjx.yupicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.yjx.yupicturebackend.config.CosClientConfig;
import com.yjx.yupicturebackend.exception.BusinessException;
import com.yjx.yupicturebackend.exception.ErrorCode;
import com.yjx.yupicturebackend.manager.CosManager;
import com.yjx.yupicturebackend.model.dto.picture.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 模版方法定义上传图片
     *
     * @param inputSource
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1.校验图片
        validPicture(inputSource);
        // 2.设置上传路径
        String originalFilename = getOriginalFilename(inputSource);
        String uuid = RandomUtil.randomString(9);
        // 自己拼接上传路径，而不是使用原始文件名，增强安全性
        String suffix = StrUtil.blankToDefault(FileUtil.getSuffix(originalFilename), "jpg");// 解决 URL 上传无法预览问题
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        // 上传文件并解析
        File file = null;
        try {
            // 3.获取临时文件
            file = File.createTempFile(uploadPath, null);
            processFile(inputSource, file);
            // 4.上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 获取图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if(CollUtil.isNotEmpty(objectList)){
                // 删除原图
                String originalKey = putObjectResult.getCiUploadResult().getOriginalInfo().getKey();
                cosManager.deleteObject(originalKey);
                // 获取压缩后得到的图片解析结果
                CIObject compressedCiObject = objectList.get(0);
                // 缩略图默认为压缩图
                CIObject thumbnailCiObject = compressedCiObject;
                // 生成了缩略图，才可以得到
                if(objectList.size() > 1){
                    thumbnailCiObject = objectList.get(1);
                };
                return buildResult(originalFilename, compressedCiObject, thumbnailCiObject, imageInfo);
            }
            // 5.封装返回结果
            return buildResult(imageInfo, putObjectResult, originalFilename, file);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //6.删除临时文件
            deleteTempFile(file);
        }
    }


    /**
     * 校验输入源
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取文件名
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 封装返回结果
     * @param originalFilename 原始文件名
     * @param compressedCiObject 压缩后的文件对象
     * @return
     */
    private UploadPictureResult buildResult(String originalFilename, CIObject compressedCiObject,
                                            CIObject thumbnailCiObject, ImageInfo imageInfo) {
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(compressedCiObject.getLocation());
        uploadPictureResult.setName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        uploadPictureResult.setThumbnailUrl(thumbnailCiObject.getLocation());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        return uploadPictureResult;
    }

    /**
     * 封装返回结果
     */
    private UploadPictureResult buildResult(ImageInfo imageInfo, PutObjectResult putObjectResult, String originalFilename, File file) {
        String picFormat = imageInfo.getFormat();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(putObjectResult.getCiUploadResult().getOriginalInfo().getLocation());
        uploadPictureResult.setName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(picFormat);
        uploadPictureResult.setPicColor(imageInfo.getAve());
        return uploadPictureResult;
    }

    /**
     * 删除临时文件
     */
    private void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filePath = {}", file.getAbsolutePath());
        }
    }
}
