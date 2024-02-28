package org.xjx.gateway.client.api;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "api")
public class ApiProperties {
    private String registerAddress;

    private String env = "dev";

    private boolean gray;
}
