package com.prestige.zephyr.migrator.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ServerInstance {
    String getName();
    String getUrl();
    String getUsername();

    @JsonIgnore
    String getPassword();
}
