package com.yjx.yupicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用分析请求
 */
@Data
public class SpaceAnalyzeRequest implements Serializable {

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 是否查询公共空间
     */
    private boolean queryPublic;

    /**
     * 是否查询所有空间
     */
    private boolean queryAll;

    @Serial
    private static final long serialVersionUID = 1L;
}
