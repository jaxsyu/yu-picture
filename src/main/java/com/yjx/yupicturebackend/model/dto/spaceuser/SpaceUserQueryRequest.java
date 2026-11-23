package com.yjx.yupicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SpaceUserQueryRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 角色: viewer, editor, admin
     */
    private String spaceRole;

    @Serial
    private static final long serialVersionUID = 1L;
}
