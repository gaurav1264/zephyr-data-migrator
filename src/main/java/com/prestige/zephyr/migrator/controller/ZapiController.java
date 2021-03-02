package com.prestige.zephyr.migrator.controller;

import com.prestige.zephyr.migrator.domain.JiraIssue;
import com.prestige.zephyr.migrator.domain.JiraProperties;
import com.prestige.zephyr.migrator.service.ZapiService;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/zapi")
public class ZapiController {
    private Logger log = LoggerFactory.getLogger(ZapiController.class);

    @Autowired
    private JiraProperties properties;

    @Autowired
    private ZapiService zServices;

    @RequestMapping(path = "/instances", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get JIRA Instance info", tags = "List all Jira instances")
    public ResponseEntity<?> getInstances() {
        log.info("instances :", properties);
        return ResponseEntity.ok(properties);
    }

    @RequestMapping(path = "/migrateTestStep", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "migrate test step", tags = "Zephyr Migration API")
    public ResponseEntity<?> migrateTestStep(@RequestParam String sourceInstance, @RequestParam String targetInstance, @RequestParam String projectKey) {
        log.info("migrateTestStep::: sourceInstance: " + sourceInstance + "  targetInstance: " + targetInstance + " projectKey:" + projectKey);
        try {
            List<JiraIssue> jIssue = zServices.getAllIssues(sourceInstance, targetInstance, projectKey);
            Map<String, String> result = zServices.migrateTestStepData(sourceInstance, targetInstance, jIssue);
            log.info("migrateTestStep::: All result: " + result);
            log.info("migrateTestStep::: Completed ");
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            log.error("migrateTestStep::: Error while migrating test step ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("migrateTestStep::: Error while migrating test step. Cause::: " + ex.getMessage());
        }
    }

    @RequestMapping(path = "/migrateTestCycle", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "migrate test Cycle", tags = "Zephyr Migration API")
    public ResponseEntity<?> migrateTestCycle(@RequestParam String sourceInstance, @RequestParam String targetInstance, @RequestParam String projectKey) {
        log.info("migrateTestStep::: sourceInstance: " + sourceInstance + "  targetInstance: " + targetInstance + " projectKey:" + projectKey);
        try {
            Map<String, String> resultMigrateTestCycle = zServices.migrateTestCycleData(sourceInstance, targetInstance, projectKey);
            log.info("migrateTestCycle::: All result: " + resultMigrateTestCycle);
            log.info("migrateTestCycle::: Completed ");
            return ResponseEntity.ok(resultMigrateTestCycle);
        } catch (Exception ex) {
            log.error("migrateTestStep::: Error while migrating test step ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("migrateTestStep::: Error while migrating test cycle. Cause::: " + ex.getMessage());
        }
    }
}