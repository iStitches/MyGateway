package org.xjx.gateway.config.center.api;

import org.xjx.common.config.Rule;

import java.util.List;

/**
 *  Rule 配置变更时触发的回调
 */
public interface RulesChangeListener {
    /**
     * 配置规则更新
     * @param rules
     */
    void onRulesChange(List<Rule> rules);
}
