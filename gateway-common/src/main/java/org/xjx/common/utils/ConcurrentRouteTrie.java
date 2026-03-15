package org.xjx.common.utils;

import org.springframework.util.StringUtils;
import org.xjx.common.config.ServiceRuleDTO;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 高并发路由前缀树 - 读写锁分离优化
 */
public class ConcurrentRouteTrie {
    static class Node {
        String part;
        Node[] children = new Node[128]; // 限制为常用字符范围
        boolean isEnd;
        boolean isWild;
        volatile ServiceRuleDTO serviceRule; // volatile保证可见性
        
        public Node(String part) {
            this.part = part;
            this.isEnd = false;
            this.isWild = false;
        }
        
        public Node(String part, boolean isWild) {
            this.part = part;
            this.children = new Node[128];
            this.isEnd = false;
            this.isWild = isWild;
        }
    }

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Node root = new Node("/");
    private volatile int routeCount = 0;

    /**
     * 批量插入路由 - 减少锁竞争
     */
    public void insertRoutes(java.util.Map<String, ServiceRuleDTO> routes) {
        lock.writeLock().lock();
        try {
            for (java.util.Map.Entry<String, ServiceRuleDTO> entry : routes.entrySet()) {
                insertRouteInternal(entry.getKey(), entry.getValue());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void insertRouteInternal(String routeName, ServiceRuleDTO serviceRule) {
        if (!StringUtils.hasText(routeName)) {
            return;
        }
        
        Node current = root;
        String[] parts = parsePath(routeName);
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            char firstChar = part.charAt(0);
            
            // 边界检查
            if (firstChar >= current.children.length) {
                continue;
            }
            
            Node child = current.children[firstChar];
            if (child == null) {
                child = new Node(part, part.charAt(0) == ':' || part.charAt(0) == '*');
                current.children[firstChar] = child;
            }
            current = child;
        }
        
        current.isEnd = true;
        current.serviceRule = serviceRule;
        routeCount++;
    }

    /**
     * 查找服务规则 - 读操作使用读锁
     */
    public ServiceRuleDTO getServiceRule(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        
        lock.readLock().lock();
        try {
            Node current = root;
            String[] parts = parsePath(path);
            
            for (String part : parts) {
                char firstChar = part.charAt(0);
                
                // 边界检查
                if (firstChar >= current.children.length) {
                    return null;
                }
                
                Node child = current.children[firstChar];
                if (child == null) {
                    return null;
                }
                
                // 优先检查通配符（性能优化）
                if (child.isWild) {
                    return child.isEnd ? child.serviceRule : null;
                }
                
                if (child.getPart().equals(part)) {
                    if (child.isEnd && child.serviceRule != null) {
                        return child.serviceRule;
                    }
                    current = child;
                } else {
                    return null;
                }
            }
            
            return current.isEnd ? current.serviceRule : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 删除路由 - 写操作使用写锁
     */
    public boolean removeRouteRule(String routeName) {
        if (!StringUtils.hasText(routeName)) {
            return false;
        }
        
        lock.writeLock().lock();
        try {
            String[] parts = parsePath(routeName);
            return removeRouteInternal(root, parts, 0);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private boolean removeRouteInternal(Node node, String[] parts, int index) {
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
        
        if (firstChar >= node.children.length) {
            return false;
        }
        
        Node child = node.children[firstChar];
        if (child == null) {
            return false;
        }
        
        if (removeRouteInternal(child, parts, index + 1)) {
            // 清理空节点
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
     * 获取统计信息 - 原子操作
     */
    public int getRouteCount() {
        return routeCount;
    }

    /**
     * 清空所有路由
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            clearNode(root);
            routeCount = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void clearNode(Node node) {
        for (int i = 0; i < node.children.length; i++) {
            if (node.children[i] != null) {
                clearNode(node.children[i]);
                node.children[i] = null;
            }
        }
        node.isEnd = false;
        node.serviceRule = null;
    }

    private String[] parsePath(String path) {
        if ("/".equals(path)) {
            return new String[]{"/"};
        }
        String[] parts = path.split("/");
        if (parts.length == 1) {
            throw new RuntimeException("route predict format is error");
        }
        return parts;
    }
}