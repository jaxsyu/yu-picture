package com.yjx.yupicturebackend.api.aliyunai.model;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建扩图任务请求类（创建任务）
 */
@Data
public class CreateOutPaintingTaskRequest implements Serializable {

    /**
     * 模型，例如 "wanx2.1-imageedit"
     */
    private String model = "wanx2.1-imageedit";

    /**
     * 输入图像信息
     */
    private Input input;

    /**
     * 图像处理参数
     */
    private Parameters parameters;

    @Data
    public static class Input {
        /**         * 必选，提示词，最长 800 个字符         */
        private String prompt = "扩展画面内容，与原图风格自然衔接";

        /**         * 必选，图像编辑功能         */
        private String function = "expand";

        /**         * 必选，原图公网 URL 或 Base64 编码         */
        @Alias("base_image_url")
        private String baseImageUrl;

        /**         * 局部重绘时必填；expand 不需要         */
        @Alias("mask_image_url")
        private String maskImageUrl;
    }

    @Data
    public static class Parameters implements Serializable {
        /**         * 向上扩展比例，范围 [1.0, 2.0]         */
        @Alias("top_scale")
        private Float topScale = 1.0f;

        /**         * 向下扩展比例，范围 [1.0, 2.0]         */
        @Alias("bottom_scale")
        private Float bottomScale = 1.0f;

        /**         * 向左扩展比例，范围 [1.0, 2.0]         */
        @Alias("left_scale")
        private Float leftScale = 1.0f;

        /**         * 向右扩展比例，范围 [1.0, 2.0]         */
        @Alias("right_scale")
        private Float rightScale = 1.0f;

        /**         * 通用参数：生成图片数量         */
        private Integer n;

        /**         * 通用参数：随机种子         */
        private Integer seed;

        /**         * 通用参数：是否添加水印         */
        private Boolean watermark;
        }
}
