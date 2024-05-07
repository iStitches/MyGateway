package org.xjx.common.utils;

import org.springframework.util.StringUtils;
import org.xjx.common.config.ServiceRuleDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *  路由前缀树
 */
public class RouteTrie {
    static class Node {
        String part;
        /**
         * 树节点
         * Key：part
         * Value：Node
         */
        Map<String, Node> children;

        /**
         * 是否匹配结束
         */
        Boolean isEnd;

        /**
         * 是否为通配符
         */
        Boolean isWild;

        /**
         * 叶子节点对应服务信息
         */
        ServiceRuleDTO serviceRule;

        public Node(String part) {
            this.part = part;
            this.children = new HashMap<>();
            this.isEnd = false;
            this.isWild = false;
        }

        public Node(String part, Boolean isWild) {
            this.part = part;
            this.children = new HashMap<>();
            this.isEnd = false;
            this.isWild = isWild;
        }
    }

    // 根节点
    private final static String ROOT_PATH = "/";

    // 服务——>根节点映射
    private final Map<String, Node> table;

    // 读写锁，并发插入服务规则时使用
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public RouteTrie() {
        table = new HashMap<>(16);
    }

    /**
     * 前缀树新增路由规则
     * @param routeName
     * @param serviceRule
     */
    public void insertRoute(String routeName, ServiceRuleDTO serviceRule) {
        try {
            lock.writeLock().lock();
            if (!StringUtils.hasText(routeName)) {
                return;
            }
            // 解析路由
            String[] parts = parsePath(routeName);
            String startRoot = parts[0];
            // 查找起始路由对应桶
            if (!table.containsKey(startRoot)) {
                table.put(startRoot, new Node(startRoot));
            }
            Node node = table.get(startRoot);
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];
                Map<String, Node> childrens = node.children;
                if (!childrens.containsKey(part)) {
                    childrens.put(part, new Node(part, part.charAt(0) == ':' || part.charAt(0) == '*'));
                }
                node = childrens.get(part);
            }
            // 遍历到了叶子节点
            node.isEnd = true;
            node.serviceRule = serviceRule;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 根据请求路径查找匹配服务信息
     * @param path
     * @return
     */
    public ServiceRuleDTO getServiceRule(String path) {
        try {
            lock.writeLock().lock();
            if (!StringUtils.hasText(path)) {
                return null;
            }
            String[] parts = parsePath(path);
            String startRoot = parts[0];
            Node node = table.get(startRoot);
            if (node == null) {
                return null;
            }
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];
                Map<String, Node> childrens = node.children;
                boolean isMatch = false;
                for (Node child : childrens.values()) {
                    if (child.isWild) {
                        return child.serviceRule;
                    }
                    if (child.part.equals(part)) {
                        isMatch = true;
                        break;
                    }
                }
                if (!isMatch) {
                    return null;
                }
                node = childrens.get(part);
            }
            return node.serviceRule;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 移除某路由匹配规则（使用栈，找到最底部然后反过来删除）
     * @param routeName
     */
    public void removeRouteRule(String routeName) {
        try {
            lock.writeLock().lock();
            ServiceRuleDTO serviceRule = getServiceRule(routeName);
            if (serviceRule == null) {
                return;
            }
            String[] parts = parsePath(routeName);
            String pathRoot = parts[0];
            // 所有 Node 压栈
            Stack<Node> stack = new Stack<>();
            Node node = table.get(pathRoot);
            stack.push(node);
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];
                Map<String, Node> childrens = node.children;
                if (!childrens.containsKey(part)) {
                    return;
                }
                node = childrens.get(part);
                stack.push(node);
            }
            stack.pop();
            // 退栈，退栈时如果发现某个节点的子节点不止一个就结束
            int deep = stack.size();
            while (!stack.isEmpty()) {
                Node top = stack.pop();
                if (top.children.size() == 1) {
                    top.children.remove(parts[deep--]);
                } else {
                    top.children.remove(parts[deep]);
                    return;
                }
            }
            // 删除根节点
            Node top = table.get(pathRoot);
            table.remove(top.part);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 解析 Path
     * @param path
     * @return
     */
    public String[] parsePath(String path) {
        if (ROOT_PATH.equals(path)) {
            return new String[] {ROOT_PATH};
        }
        String[] parts = path.split("/");
        if (parts.length == 1) {
            throw new RuntimeException("route predict format is error");
        }
        return parts;
    }
}
