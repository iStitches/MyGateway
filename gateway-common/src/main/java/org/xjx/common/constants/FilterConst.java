package org.xjx.common.constants;

public interface FilterConst {
    // 负载均衡过滤器
    String LOAD_BALANCE_FILTER_ID = "load_balance_filter";
    String LOAD_BALANCE_FILTER_NAME = "load_balance_filter";
    int LOAD_BALANCE_FILTER_ORDER = 100;

    String LOAD_BALANCE_KEY = "load_balance";
    String LOAD_BALANCE_STRATEGY_RANDOM = "Random";
    String LOAD_BALANCE_STRATEGY_ROUND_ROBIN = "RoundRobin";

    // 路由过滤器
    String ROUTER_FILTER_ID = "router_filter";
    String ROUTER_FILTER_NAME = "router_filter";
    int ROUTER_FILTER_ORDER = Integer.MAX_VALUE;

    // 限流过滤器
    String FLOW_CTL_FILTER_ID = "flow_ctl_filter";
    String FLOW_CTL_FILTER_NAME = "flow_ctl_filter";
    int FLOW_CTL_FILTER_ORDER = 50;
    String FLOW_CTL_TYPE_PATH = "path";
    String FLOW_CTL_TYPE_SERVICE = "service";

    String FLOW_CTL_LIMIT_DURATION = "duration";    //限流时间单位——秒
    String FLOW_CTL_LIMIT_PERMITS = "permits";      //限流请求次数——次
    String FLOW_CTL_MODE_DISTRIBUTED = "distributed"; //分布式场景
    String FLOW_CTL_MODE_SINGLETON = "singleton";     //单例场景

    // 认证鉴权过滤器
    String AUTH_FILTER_ID = "auth_filter";
    String AUTH_FILTER_NAME = "auth_filter";
    String AUTH_FILTER_KEY = "auth_path";
    int AUTH_FILTER_ORDER = 2;
    String TOKEN_SECRET = "xjx123456";
    String COOKIE_KEY = "mygateway-jwt";

    // 灰度发布过滤器
    String GRAY_FILTER_ID = "gray_filter";
    String GRAY_FILTER_NAME = "gray_filter";
    int GRAY_FILTER_ORDER = 1;
    String GRAY_FILTER_KEY = "gray_release";
}
