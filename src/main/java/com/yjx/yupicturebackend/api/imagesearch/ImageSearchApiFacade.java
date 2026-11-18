package com.yjx.yupicturebackend.api.imagesearch;

import com.yjx.yupicturebackend.api.imagesearch.model.ImageSearchResult;
import com.yjx.yupicturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.yjx.yupicturebackend.api.imagesearch.sub.GetImageListApi;
import com.yjx.yupicturebackend.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {

    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        return GetImageListApi.getImageList(imageFirstUrl);
    }

    public static void main(String[] args) {
        // 测试以图搜图
        String imageUrl = "https://yujax-1328217318.cos.ap-guangzhou.myqcloud.com/public/2035320769225789441/2026-11-14_TJg92whUC.webp";
        List<ImageSearchResult> resultList = searchImage(imageUrl);
        System.out.println("结果列表：" + resultList);
    }
}
