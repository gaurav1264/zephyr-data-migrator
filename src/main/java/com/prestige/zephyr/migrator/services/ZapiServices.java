package com.prestige.zephyr.migrator.services;

import com.prestige.zephyr.migrator.Domain.JiraProperties;
import com.prestige.zephyr.migrator.controller.ZapiController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

public class ZapiServices {
    private Logger log = LoggerFactory.getLogger(ZapiServices.class);

    @Autowired
    private JiraProperties properties;

    @Autowired
    private RestTemplate rawRestTemplate;

    @Autowired
    private RequestBuilder<String> requestBuilder;
}
