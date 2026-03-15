package org.xjx.common.utils;

import org.springframework.util.StringUtils;
import org.xjx.common.config.ServiceRuleDTO;

/**
 * 优化的路由前缀树 - 使用数组替代HashMap提升性能
 */
public class OptimizedRouteTrie {
    static class Node {
        char[] part; // 存储路径片段
        Node[] children = new Node[256]; // ASCII字符数组，直接寻址
        boolean isEnd;
        boolean isWild;
        ServiceRuleDTO serviceRule;
        
        public Node(char[] part) {
            this.part = part;
            this.isEnd = false;
            this.isWild = false;
        }
        
        public Node(char[] part, boolean isWild) {
            this.part = part;
            this.isEnd = false;
            this.isWild = isWild;
        }
        
        public String getPart() {
            return new String(part);
        }
    }

    private final static String ROOT_PATH = "/";
    private final Node root = new Node(ROOT_PATH.toCharArray());
    private int routeCount = 0;

    /**
     * 插入路由规则 - 性能优化版本
     */
    public void insertRoute(String routeName, ServiceRuleDTO serviceRule) {
        if (!StringUtils.hasText(routeName)) {
            return;
        }
        
        Node current = root;
        String[] parts = parsePath(routeName);
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            char firstChar = part.charAt(0);
            
            // 直接使用ASCII码作为索引，避免HashMap查找开销
            Node child = current.children[firstChar];
            if (child == null) {
                child = new Node(part.toCharArray(), part.charAt(0) == ':' || part.charAt(0) == '*');
                current.children[firstChar] = child;
            }
            current = child;
        }
        
        current.isEnd = true;
        current.serviceRule = serviceRule;
        routeCount++;
    }

    /**
     * 查找服务规则 - 性能优化版本  
     */
    public ServiceRuleDTO getServiceRule(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        
        Node current = root;
        String[] parts = parsePath(path);
        
        for (String part : parts) {
            char firstChar = part.charAt(0);
            Node child = current.children[firstChar];
            
            if (child == null) {
                return null;
            }
            
            // 检查通配符或精确匹配
            if (child.isWild || child.getPart().equals(part)) {
                if (child.isEnd && child.serviceRule != null) {
                    return child.serviceRule;
                }
                current = child;
            } else {
                return null;
            }
        }
        
        return current.isEnd ? current.serviceRule : null;
    }

    /**
     * 删除路由规则 - 优化版本
     */
    public boolean removeRouteRule(String routeName) {
        if (!StringUtils.hasText(routeName)) {
            return false;
        }
        
        String[] parts = parsePath(routeName);
        return removeRoute(root, parts, 0);
    }
    
    private boolean removeRoute(Node node, String[] parts, int index) {
        if (index >= parts.length) {
            if (node.isEnd) {
                node.isEnd = false;
                node.serviceRule = null;
                routeCount--;
                return true;
            }
            return false;
        }
        
        String part = parts[index];
        char firstChar = part.charAt(0);
        Node child = node.children[firstChar];
        
        if (child == null) {
            return false;
        }
        
        if (removeRoute(child, parts, index + 1)) {
            // 如果子节点没有其他引用且不是叶子节点，可以清理
            if (!child.isEnd && !hasChildren(child)) {
                node.children[firstChar] = null;
            }
            return true;
        }
        
        return false;
    }
    
    private boolean hasChildren(Node node) {
        for (Node child : node.children) {
            if (child != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 批量插入路由规则
     */
    public void insertRoutes(java.util.Map<String, ServiceRuleDTO> routes) {
        routes.forEach(this::insertRoute);
    }

    /**
     * 获取路由统计信息
     */
    public int getRouteCount() {
        return routeCount;
    }

    /**
     * 清空所有路由
     */
    public void clear() {
        clearNode(root);
        routeCount = 0;
    }
    
    private void clearNode(Node node) {
        for (Node child : node.children) {
            if (child != null) {
                clearNode(child);
            }
        }
        node.children = new Node[256];
        node.isEnd = false;
        node.serviceRule = null;
    }

    /**
     * 解析路径 - 复用逻辑
     */
    private String[] parsePath(String path) {
        if (ROOT_PATH.equals(path)) {
            return new String[]{ROOT_PATH};
        }
        String[] parts = path.split("/");
        if (parts.length == 1) {
            throw new RuntimeException("route predict format is error");
        }
        return parts;
    }
}