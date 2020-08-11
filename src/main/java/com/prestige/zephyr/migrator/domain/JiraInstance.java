package com.prestige.zephyr.migrator.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "instances")
public class JiraInstance implements ServerInstance {
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String url;
    @Getter
    @Setter
    private String username;
    @Getter
    @Setter
    private String password;
}
