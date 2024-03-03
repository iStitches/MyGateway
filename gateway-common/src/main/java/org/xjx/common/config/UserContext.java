package org.xjx.common.config;

import java.util.HashMap;
import java.util.Map;
public class UserContext {

    private static Map<String, UserInfoDTO> userInfoMap;

    public UserContext() {
        userInfoMap = new HashMap<>();
    }

    private static class SingletonContext {
        private static final UserContext instance = new UserContext();
    }

    public static UserContext getInstance() {
        System.out.println("UserContext hashcode is " + SingletonContext.instance.hashCode());
        return SingletonContext.instance;
    }

    public static void setUserInfoDTO(String userId, UserInfoDTO info) {
        getInstance().userInfoMap.put(userId, info);
    }

    public static UserInfoDTO getUserInfo(String userId) {
        return getInstance().userInfoMap.get(userId);
    }

    public static Map<String, UserInfoDTO> getUserInfoMap() {
        return userInfoMap;
    }
}