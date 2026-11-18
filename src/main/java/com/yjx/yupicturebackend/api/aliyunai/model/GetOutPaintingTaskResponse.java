package com.yjx.yupicturebackend.api.aliyunai.model;

import cn.hutool.core.annotation.Alias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 查询任务响应类（根据获取的任务ID查询结果）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetOutPaintingTaskResponse {

    /**
     * 请求唯一标识
     */
    @Alias("request_id")
    private String requestId;

    /**
     * 输出信息
     */
    private Output output;

    /**
     * 用量信息
     */
    private Usage usage;

    /**
     * 表示任务的输出信息
     */
    @Data
    public static class Output {

        /**
         * 任务 ID
         */
        @Alias("task_id")
        private String taskId;

        /**
         * 任务状态
         * <ul>
         *     <li>PENDING：排队中</li>
         *     <li>RUNNING：处理中</li>
         *     <li>SUSPENDED：挂起</li>
         *     <li>SUCCEEDED：执行成功</li>
         *     <li>FAILED：执行失败</li>
         *     <li>UNKNOWN：任务不存在或状态未知</li>
         * </ul>
         */
        @Alias("task_status")
        private String taskStatus;

        /**
         * 提交时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        @Alias("submit_time")
        private String submitTime;

        /**
         * 调度时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        @Alias("scheduled_time")
        private String scheduledTime;

        /**
         * 结束时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        @Alias("end_time")
        private String endTime;

        /**
         * 输出结果列表（成功后包含生成图像的 URL）
         */
        private List<Result> results;


        /**
         * 任务指标信息
         */
        @Alias("task_metrics")
        private TaskMetrics taskMetrics;
    }

    /**
     * 表示单条输出结果
     * <p>成功项：url 有值；失败项：code、message 有值</p>
     */
    @Data
    public static class Result {

        /**
         * 生成图像的 URL（成功时返回）
         */
        private String url;

        /**
         * 错误码（该条结果执行失败时返回）
         */
        private String code;

        /**
         * 错误信息（该条结果执行失败时返回）
         */
        private String message;

        /**
         * 判断该条结果是否成功
         */
        public boolean isSuccess() {
            return url != null && !url.isEmpty();
        }
    }

    /**
     * 表示任务的统计信息
     */
    @Data
    public static class TaskMetrics {

        /**
         * 总任务数
         */
        @Alias("TOTAL")
        private Integer total;

        /**
         * 成功任务数
         */
        @Alias("SUCCEEDED")
        private Integer succeeded;

        /**
         * 失败任务数
         */
        @Alias("FAILED")
        private Integer failed;
    }

    /**
     * 表示用量信息
     */
    @Data
    public static class Usage {

        /**
         * 生成图像数量
         */
        @Alias("image_count")
        private Integer imageCount;
    }
}
