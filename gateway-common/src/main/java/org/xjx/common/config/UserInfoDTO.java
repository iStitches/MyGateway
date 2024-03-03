package org.xjx.common.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户发起请求后的唯一用户ID（考虑雪花算法是西安）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoDTO {
    private long user_id;
    private String rsa_privateKey;
}
