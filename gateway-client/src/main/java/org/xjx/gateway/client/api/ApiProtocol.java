package org.xjx.gateway.client.api;

public enum ApiProtocol {
    HTTP("http","http协议"),
    DUBBO("dubbo","dubbo协议");
    private String code;
    private String desc;

    ApiProtocol(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
