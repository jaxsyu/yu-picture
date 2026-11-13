package com.yjx.yupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = -5052283543722998454L;

    private String userAccount;

    private String userPassword;
}
