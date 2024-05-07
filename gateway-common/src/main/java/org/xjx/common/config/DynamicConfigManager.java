package org.xjx.common.config;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.xjx.common.utils.RouteTrie;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 配置信息缓存类
 * 1.缓存从配置中心获取的配置信息（Rule规则配置、Service服务配置）；
 * 2.动态更新配置信息；
 */
public class DynamicConfigManager {
    // 服务定义信息集合  serviceId——>ServiceDefinition
    private ConcurrentHashMap<String, ServiceDefinition> serviceDefinitionMap = new ConcurrentHashMap<>();
    // 服务实例集合     serviceId——>Set<ServiceInstance>
    private ConcurrentHashMap<String, Set<ServiceInstance>> serviceInstanceMap = new ConcurrentHashMap<>();
    // 规则集合        ruleId——>Rule
    private ConcurrentHashMap<String, Rule> ruleMap = new ConcurrentHashMap<>();
    // 路径以及规则集合  serviceId.requestPath——>Rule
    private ConcurrentHashMap<String, Rule> pathRuleMap = new ConcurrentHashMap<>();
    // 路径集合        service——>List<Rule>
    private ConcurrentHashMap<String, List<Rule>> serviceRuleMap = new ConcurrentHashMap<>();
    // 路由规则缓存——前缀树
    private static final RouteTrie ROUTE_RULE_ROUTE_TRIE = new RouteTrie();

    public DynamicConfigManager() {
    }

    /**
     * 新增前缀路由
     * @param map
     */
    public static void addRule(Map<String, ServiceRuleDTO> map) {
        for (Map.Entry<String, ServiceRuleDTO> entry : map.entrySet()) {
            String routeName = entry.getKey();
            ServiceRuleDTO serviceRuleDTO = entry.getValue();
            ROUTE_RULE_ROUTE_TRIE.insertRoute(routeName, entry.getValue());
        }
    }

    /**
     * 更新前缀路由
     * @param map
     */
    public static void updateRule(Map<String, ServiceRuleDTO> map) {
        addRule(map);
    }

    /**
     * 删除前缀路由
     * @param routeName
     */
    public static void removeRule(String routeName) {
        ROUTE_RULE_ROUTE_TRIE.removeRouteRule(routeName);
    }

    /**
     * 根据请求路径查找路由规则
     * @param path
     */
    public static ServiceRuleDTO getServiceRule(String path) {
        return Optional.ofNullable(ROUTE_RULE_ROUTE_TRIE.getServiceRule(path))
                .orElseThrow(()->new RuntimeException("routeName: " + path + " not found"));
    }

    private static class SingletonHolder {
        private static final DynamicConfigManager INSTANCE = new DynamicConfigManager();
    }

    public static DynamicConfigManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /******* 对服务定义缓存的相关方法 ********/
    public void putServiceDefinition(String uniqueId, ServiceDefinition definition) {
        serviceDefinitionMap.put(uniqueId, definition);
    }

    public void removeServiceDefinition(String uniqueId) {
        serviceDefinitionMap.remove(uniqueId);
    }

    public ServiceDefinition getServiceDefinition(String uniqueId) {
        if (StringUtils.isEmpty(uniqueId)) {
            return null;
        }
        return serviceDefinitionMap.get(uniqueId);
    }

    public ConcurrentHashMap<String, ServiceDefinition> getServiceDefinitionMap() {
        return serviceDefinitionMap;
    }

    /******* 对服务实例缓存的相关方法 ********/
    public void putServiceInstance(String uniqueId, ServiceInstance instance) {
        Set<ServiceInstance> instanceSet = serviceInstanceMap.get(uniqueId);
        instanceSet.add(instance);
    }

    public void putServiceInstance(String uniqueId, Set<ServiceInstance> instanceSet) {
        serviceInstanceMap.put(uniqueId, instanceSet);
    }

    /**
     * 根据服务ID获取服务实例
     * @param uniqueId
     * @param gray
     * @return
     */
    public Set<ServiceInstance> getServiceInstanceByUniqueId(String uniqueId, boolean gray) {
        Set<ServiceInstance> instanceSet = serviceInstanceMap.get(uniqueId);
        if (CollectionUtils.isEmpty(instanceSet)) {
            return Collections.EMPTY_SET;
        }
        // 为灰度流量,返回灰度服务实例
        if (gray) {
            return instanceSet.stream().filter(ServiceInstance::isGray).collect(Collectors.toSet());
        }
        return instanceSet;
    }

    public void updateServiceInstance(String uniqueId, ServiceInstance instance) {
        Set<ServiceInstance> instanceSet = serviceInstanceMap.get(uniqueId);
        Iterator<ServiceInstance> it = instanceSet.iterator();
        while(it.hasNext()) {
            ServiceInstance next = it.next();
            if (next.getServiceInstanceId().equals(instance.getServiceInstanceId())) {
                it.remove();
                break;
            }
        }
        instanceSet.add(instance);
    }

    public void removeServiceInstance(String uniqueId, String serviceInstanceId) {
        Set<ServiceInstance> instanceSet = serviceInstanceMap.get(uniqueId);
        Iterator<ServiceInstance> iterator = instanceSet.iterator();
        while (iterator.hasNext()) {
            ServiceInstance next = iterator.next();
            if (next.getServiceInstanceId().equals(serviceInstanceId)) {
                iterator.remove();
                break;
            }
        }
    }

    public void removeServiceInstanceByUniqueId(String uniqueId) {
        serviceInstanceMap.remove(uniqueId);
    }

    /******* 缓存规则相关操作方法 ********/
    public void putRule(String ruleId, Rule rule) {
        ruleMap.put(ruleId, rule);
    }

    public void putAllRule(List<Rule> ruleList) {
        ConcurrentHashMap<String, Rule> newRuleMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Rule> newPathMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, List<Rule>> newServiceMap = new ConcurrentHashMap<>();
        for (Rule rule : ruleList) {
            newRuleMap.put(rule.getId(), rule);
            List<Rule> rules = newServiceMap.get(rule.getServiceId());
            if (rules == null) {
                rules = new ArrayList<>();
            }
            rules.add(rule);
            newServiceMap.put(rule.getServiceId(), rules);

            List<String> paths = rule.getPaths();
            for (String path : paths) {
                String key = rule.getServiceId() + "." + path;
                newPathMap.put(key, rule);
            }
        }
        ruleMap = newRuleMap;
        pathRuleMap = newPathMap;
        serviceRuleMap = newServiceMap;
    }

    public Rule getRule(String ruleId) {
        return ruleMap.get(ruleId);
    }

    public ConcurrentHashMap<String, Rule> getRuleMap() {
        return ruleMap;
    }

    public Rule getRulePath(String path) {
        return pathRuleMap.get(path);
    }

    public List<Rule> getRuleByServiceId(String serviceId) {
        return serviceRuleMap.get(serviceId);
    }

//    class Node {
//        // 路由路径
//        String path;
//        // 路由中由 '/' 分隔的部分，比如 /hello/:name，part为 hello、:name
//        String part;
//        // 子节点路径映射
//        HashMap<String, Node> children;
//        // 是否根节点
//        boolean isEnd;
//        // 是否精确匹配，true代表模糊匹配
//        boolean isWild;
//
//        public Node() {
//            this.children = new HashMap<>();
//            this.isEnd = false;
//        }
//
//        public void setPath(String path) {
//            this.path = path;
//        }
//
//        public void setPart(String part) {
//            this.part = part;
//        }
//
//        public void setChildren(HashMap<String, Node> children) {
//            this.children = children;
//        }
//
//        public void setEnd(boolean end) {
//            isEnd = end;
//        }
//    }
//
//    class Router {
//        // 请求方式—前缀树根节点 映射
//        HashMap<String, Node> root;
//        // 请求方式-请求路径 到API配置映射
//        HashMap<String, List<Rule>> route;
//
//        public Router() {
//            this.root = new HashMap<>();
//            this.route = new HashMap<>();
//        }
//
//        public HashMap<String, Node> getRoot() {
//            return root;
//        }
//
//        public void setRoot(HashMap<String, Node> root) {
//            this.root = root;
//        }
//
//        public HashMap<String, List<Rule>> getRoute() {
//            return route;
//        }
//
//        public void setRoute(HashMap<String, List<Rule>> route) {
//            this.route = route;
//        }
//    }
//
//    private Router initTire() {
//        return new Router();
//    }
//
//    /**
//     * 拆分 path 为词
//     * @param path
//     * @return
//     */
//    private List<String> parsePath(String path) {
//        String[] parts = path.split("/");
//        List<String> list = new ArrayList<>();
//        for (String part : parts) {
//            if (part != "") {
//                list.add(part);
//                if (part.startsWith("*")) {
//                    break;
//                }
//            }
//        }
//        return list;
//    }
//
//    /**
//     * 插入请求URL——API配置映射
//     * @param path
//     * @param method
//     * @param router
//     * @param ruleList
//     * @return
//     */
//    private boolean insertNode(String path, String method, Router router, List<Rule> ruleList) {
//        List<String> paths = parsePath(path);
//        if (CollectionUtils.isEmpty(paths)) {
//            return false;
//        }
//        Node node = router.getRoot().get(method);
//        String key = method + "-" + path;
//        for (String part : paths) {
//            if (node.children.get(part) == null) {
//                Node tmp = new Node();
//                tmp.setPart(part);
//                node.children.put(part, tmp);
//            }
//            node = node.children.get(part);
//        }
//        node.path = path;
//        router.route.put(key, ruleList);
//        return true;
//    }
//
//    /**
//     * 根据请求路径查找前缀树，获取节点和请求参数信息
//     * @param method
//     * @param path
//     * @return
//     */
//    private Map<String, Object> getRoute(String method, String path, Router router) {
//        Map<String, Object> ans = new HashMap<>();
//        Map<String, String> params = new HashMap<>();
//        List<String> parts = parsePath(path);
//        // 获取请求方式对应前缀树根节点
//        Node root = router.getRoot().get(method);
//        if (root == null) {
//            return null;
//        }
//        // 迭代查找
//        for (int i = 0; i < parts.size(); i++) {
//            String part = parts.get(i);
//            String temp = null;
//            // 判断是否找到匹配前缀
//            if (root.children.containsKey(part)) {
//                // 模糊匹配，后面都可以匹配上
//                if (part.startsWith("*")) {
//                    String[] lefts = (String[]) Arrays.copyOfRange(parts.toArray(), i, parts.size() - 1);
//                    StringBuilder builder = new StringBuilder();
//                    for (String str : lefts) {
//                        builder.append(str);
//                    }
//                    params.put(part.substring(1), builder.toString()+"/");
//                } else if (part.startsWith(":")) {
//                // 路径含有可替换参数
//                    params.put(part.substring(1), part);
//                }
//                temp = part;
//            }
//            // 遇到通配符*，直接返回
//            if (temp.charAt(0) == '*') {
//                ans.put("node", root.children.get(temp));
//                ans.put("params", params);
//                return ans;
//            }
//            root = root.children.get(temp);
//        }
//        ans.put("node", root);
//        ans.put("params", params);
//        return ans;
//    }
}