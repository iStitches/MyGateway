package org.xjx.gateway.starter.core.snowflake;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkIdWrapper {
    private Long workId;
    private Long dataCenterId;
}
