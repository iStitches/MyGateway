package org.xjx.gateway.pojo.params;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 系统初始化参数
 */
@Data
public class InitParam {
    @NotBlank
    private Integer centerType;
    @NotBlank
    private String ServerAddr;
}
