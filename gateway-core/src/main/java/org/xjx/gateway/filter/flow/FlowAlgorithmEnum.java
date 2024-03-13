package org.xjx.gateway.filter.flow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 限流算法枚举
 */
@RequiredArgsConstructor
public enum FlowAlgorithmEnum {
    VOTE_BUCKET_ALGORITHM(1, "vote_bucket"),
    STREAM_BUCKET_ALGORITHM(2, "stream_bucket"),
    MOVE_WINDOWS_ALGORITHM(3, "move_window"),
    FIXED_WINDOWS_ALGORITHM(4, "fixed_window");

    @Getter
    private final Integer code;

    @Getter
    private final String alg;
}
