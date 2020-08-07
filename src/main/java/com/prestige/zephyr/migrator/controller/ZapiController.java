package com.prestige.zephyr.migrator.controller;

import com.prestige.zephyr.migrator.Domain.JiraProperties;
import com.prestige.zephyr.migrator.services.ZapiServices;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

//import java.util.logging.Logger;
//import java.util.logging.Logger;

@RestController
@RequestMapping("/zapi")
public class ZapiController {
    private Logger log = LoggerFactory.getLogger(ZapiController.class);

    @Autowired
    private JiraProperties properties;

    @Autowired
    private ZapiServices zServices;

    @RequestMapping(path="/info",method = RequestMethod.GET)
    @ApiOperation(value = "basic zapi info")
    public ResponseEntity getInfo(){
        return ResponseEntity.ok("All is well");
    }
    @RequestMapping(path="/instances",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get JIRA Instance info",tags = "Zephyr MigrationAPI")
    public ResponseEntity<?> getInstances(){
        log.info("instances :" , properties);
        return ResponseEntity.ok(properties);
    }
    @RequestMapping(path="/migrateTestStep/{sourceInstance}/{targetInstance}/{projectKey}",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "migrate test step",tags = "Zephyr MigrationAPI")
    public ResponseEntity<?> migrateTestStep(String sourceInstance,String targetInstance,String projectKey){
        return ResponseEntity.ok("All is well");
    }

}