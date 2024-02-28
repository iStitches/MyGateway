package org.xjx.common.utils;

import io.netty.channel.Channel;

import java.net.SocketAddress;

public class RemotingHelper {
    /**
     * 解析 Channel 远程地址
     * @param channel
     * @return
     */
    public static String parseChannelRemoteAddr(final Channel channel) {
        if (channel == null) {
            return "";
        }
        SocketAddress address = channel.remoteAddress();
        final String addrStr = address != null ? address.toString() : "";
        if (addrStr.length() > 0) {
            int pos = addrStr.lastIndexOf("/");
            if (pos >= 1) {
                return addrStr.substring(pos+1);
            }
            return addrStr;
        }
        return "";
    }
}
