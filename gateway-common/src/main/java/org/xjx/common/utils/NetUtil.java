package org.xjx.common.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetUtil {
    public static String getLocalIp() {
        return getLocalIp("*>10>172>192>127");
    }

    public static int matchIndex(String ip, String[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            String p = prefix[i];
            if ("*".equals(p)) {
                if (ip.startsWith("127.") || ip.startsWith("192.") || ip.startsWith("172.") || ip.startsWith("10.")) {
                    continue;
                }
                return i;
            } else {
                if (ip.startsWith(p)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 获取本地IP地址
     * @param ipReference
     * @return
     */
    public static String getLocalIp(String ipReference) {
        if (ipReference == null) {
            ipReference = "*>10>172>192>127";
        }
        String[] prefixs = ipReference.split("[> ]+");

        try {
            Pattern pattern = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+");
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            int matchedIdx = -1;
            String matchedIp = null;
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface nt = networkInterfaces.nextElement();
                // 跳过虚拟网卡IP
                if (nt.isLoopback() || nt.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> en = nt.getInetAddresses();
                while (en.hasMoreElements()) {
                    InetAddress addr = en.nextElement();
                    if (addr.isLoopbackAddress() || !addr.isSiteLocalAddress() || addr.isAnyLocalAddress()) {
                        continue;
                    }
                    String ip = addr.getHostAddress();
                    Matcher matcher = pattern.matcher(ip);
                    if (matcher.matches()) {
                        int idx = matchIndex(ip, prefixs);
                        if (idx == -1) {
                            continue;
                        }
                        if (matchedIdx == -1) {
                            matchedIdx = idx;
                            matchedIp = ip;
                        } else {
                            if (matchedIdx > idx) {
                                matchedIdx = idx;
                                matchedIp = ip;
                            }
                        }
                    }
                }
            }
            if (matchedIp != null) {
                return matchedIp;
            }
            return "127.0.0.1";
        } catch (SocketException e) {
            return "127.0.0.1";
        }
    }
}
