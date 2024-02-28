package org.xjx.gateway.http.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfo {
    int id;
    String phoneNumber;
    String name;
    String passwd;
}
