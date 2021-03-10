package com.prestige.zephyr.migrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prestige.zephyr.migrator.domain.*;
import com.prestige.zephyr.migrator.utility.AppUtils;
import com.prestige.zephyr.migrator.utility.InstanceHelper;
import com.prestige.zephyr.migrator.utility.RequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.codehaus.jettison.json.*;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ZapiService {
    private Logger log = LoggerFactory.getLogger(ZapiService.class);

    @Autowired
    private JiraProperties properties;

    @Autowired
    private RestTemplate rawRestTemplate;

    @Autowired
    private RequestBuilder<String> requestBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    public static Map<String, List<TestCycleIssue>> executionDetailsCache = new HashMap<>();

    public List<JiraIssue> getAllIssues(String srcInstance, String targetInstance, String projectKey) throws Exception {
        try {
            log.info("Get all issues for giving source instances:" + srcInstance + " & project Key :" + projectKey);
            Map<String, Long> srcIssuesByKeyId = getIssuesByProjectKey(srcInstance, projectKey);
            log.info("Get all issues for giving target instances:" + srcInstance + " & project Key :" + projectKey);
            Map<String, Long> targetIssuesByKeyId = getIssuesByProjectKey(targetInstance, projectKey);
            // if everything is ok above merge the two to form the jiraissue object
            if (srcIssuesByKeyId.size() > targetIssuesByKeyId.size()) {
                log.error("getAllIssues::: Mismatch in zephyr  test issue in source and destination");
            }
            List<JiraIssue> issues = new ArrayList<>();
            for (String key : srcIssuesByKeyId.keySet()) {
                JiraIssue issue = new JiraIssue();
                issue.setIssueKey(key);
                issue.setOldIssueId(srcIssuesByKeyId.get(key));
                issue.setNewIssueId(targetIssuesByKeyId.get(key));
                issues.add(issue);
            }
            return issues;
        } catch (Exception ex) {
            log.error("getAllIssues::: Error While getting all issues ", ex);
            throw ex;
        }


    }

    private Map<String, Long> getIssuesByProjectKey(String instance, String projectKey) throws Exception {
        Map<String, Long> issuesByKeyId = new TreeMap<>();
        try {
            JiraInstance jInstance = InstanceHelper.getInstanceDetailsByName(instance, properties);
            String endPointUrl = "/rest/api/2/search?jql=project=" + projectKey + " AND issuetype=TEST ORDER BY key&maxResults=0";
            String endPoint = AppUtils.getEndPoint(jInstance.getUrl(), endPointUrl);
            log.info("getIssuesByProjectKey::: endPoint:" + endPoint);
            String response = rawRestTemplate.exchange(endPoint, HttpMethod.GET,
                    requestBuilder.withAuthHeaderJson(jInstance.getUsername(), jInstance.getPassword()), String.class).getBody();
            log.info("getIssuesByProjectKey::: response:" + response);
            JSONObject jsonObject = new JSONObject(response);
            int maxResults = 1000;
            int total = jsonObject.getInt("total");
            int iteration = total / maxResults;
            int modulo = total % maxResults;
            if (modulo > 0) {
                iteration = iteration + 1;
            }
            log.info("getIssuesByProjectKey::: Total Number of issues :" + total + " ,Total iteration : " + iteration);
            int startAt = 0;
            for (int i = 1; i < iteration; i++) {
                log.info("getIssuesByProjectKey::: Current Iteration :" + i + " /" + iteration);
                JSONArray issues = getIssuesByJQL(instance, projectKey, startAt, maxResults);
                for (int index = 0; index < issues.length(); index++) {
                    JSONObject issue = issues.getJSONObject(index);
                    issuesByKeyId.put(issue.getString("key"), issue.getLong("id"));
                }
                startAt = startAt + maxResults;
            }

        } catch (Exception ex) {
            log.error("getIssuesByProjectKey::: Error While getting issues using JQL using project key ", ex);
            throw ex;
        }
        return issuesByKeyId;
    }

    private JSONArray getIssuesByJQL(String instance, String projectKey, int start, int maxResult) throws Exception {

        try {
            JiraInstance jInstance = InstanceHelper.getInstanceDetailsByName(instance, properties);
            String endPointUrl = "/rest/api/2/search?jql=project=" + projectKey + " AND issuetype=TEST ORDER BY key&fields=*none&maxResults=" + maxResult + "&startAt=" + start;
            String endPoint = AppUtils.getEndPoint(jInstance.getUrl(), endPointUrl);
            log.info("getIssuesByJQL::: endPoint:" + endPoint);
            String response = rawRestTemplate.exchange(endPoint, HttpMethod.GET,
                    requestBuilder.withAuthHeaderJson(jInstance.getUsername(), jInstance.getPassword()), String.class).getBody();
            log.info("getIssuesByJQL::: response:" + endPoint);
            JSONObject jObject = new JSONObject(response);
            JSONArray jsonArray = jObject.getJSONArray("issues");
            return jsonArray;
        } catch (Exception ex) {
            log.error("getIssuesByJQL::: Error While getting issues using JQL ", ex);
            throw ex;
        }

    }

    public Map<String, String> migrateTestStepData(String sourceInstance, String targetInstance, List<JiraIssue> issues) {
        Map<String, String> result = new TreeMap<>();
        log.info("migrateTestStepData::: sourceInstance: " + sourceInstance + "  targetInstance: " + targetInstance);
        int size = issues.size() + 1;
        int index = 1;
        for (JiraIssue issue : issues) {
            log.info("migrateTestStepData::: migrating Issue Details: " + issue);
            log.info("migrateTestStepData::: Currently Processing: " + index + " of total issues : " + size);
            try {
                //get step details from the source instance;
                List<Map<String, String>> sourceSteps = getTestStepDetails(sourceInstance, issue.getIssueKey(), issue.getOldIssueId());
                if (sourceSteps.size() > 0) {
                    List<Map<String, String>> targetSteps = getTestStepDetails(targetInstance, issue.getIssueKey(), issue.getNewIssueId());
                    try {
                        if (targetSteps.size() > 0) {
                            log.info("migrateTestStepData::: Test Step Data already found in target issue" + issue.getIssueKey());
                            result.put(issue.getIssueKey(), "Test Step Data already found in target issue");
                        } else {
                            createStepsinTargetIssue(targetInstance, issue, sourceSteps);
                            log.info("migrateTestStepData::: Test steps migrated Successfully" + issue.getIssueKey());
                            result.put(issue.getIssueKey(), "Test steps migrated Successfully");
                        }
                    } catch (Exception ex) {
                        log.info("migrateTestStepData::: Some Error while migrating, Please check manually" + issue.getIssueKey());
                        result.put(issue.getIssueKey(), "Some Error while migrating, Please check manually");
                    }
                } else {
                    log.info("migrateTestStepData::: No step data found for issue" + issue.getIssueKey());
                    result.put(issue.getIssueKey(), "No step data found for migration");
                }
            } catch (Exception ex) {
                log.info("migrateTestStepData::: Error while migrating data" + issue.getIssueKey());
                result.put(issue.getIssueKey(), "Internal Error, Check Manually");
            }
        }
        return result;
    }

    private void createStepsinTargetIssue(String instance, JiraIssue issue, List<Map<String, String>> testSteps) throws Exception {
        try {
            JiraInstance jInstance = InstanceHelper.getInstanceDetailsByName(instance, properties);
            String endPointUrl = "/rest/zapi/latest/teststep/" + issue.getNewIssueId();
            String endPoint = AppUtils.getEndPoint(jInstance.getUrl(), endPointUrl);
            log.info("createStepsinTargetIssue::: endPoint:" + endPoint);
            for (Map<String, String> tsteps : testSteps) {
                String jsonInString = removeUnicodeChar(objectMapper.writeValueAsString(tsteps));
                String response = rawRestTemplate.exchange(endPoint, HttpMethod.POST,
                        requestBuilder.withAuthHeaderandBody(jInstance.getUsername(), jInstance.getPassword(), jsonInString), String.class).getBody();
                log.info("createStepsinTargetIssue::: response:" + endPoint);
                //JSONObject jObject = new JSONObject(response);
            }
        } catch (Exception ex) {
            log.info("createStepsinTargetIssue::: Error :", ex);
            throw ex;
        }
    }

    private String removeUnicodeChar(String inputString) {
        String convertedString = Normalizer.normalize(inputString, Normalizer.Form.NFKD);
        convertedString = convertedString.replaceAll("[^\\\\p{Print}]", "");
        return convertedString;
    }

    private List<Map<String, String>> getTestStepDetails(String instance, String issueKey, Long issueId)
            throws Exception {
        List<Map<String, String>> steps = new ArrayList<>();
        log.info("getTestStepDetails::: Instance: " + instance + " IssueKey : " + issueKey);
        try {
            JiraInstance jInstance = InstanceHelper.getInstanceDetailsByName(instance, properties);
            String endPointUrl = "/rest/zapi/latest/teststep/" + issueId;
            String endPoint = AppUtils.getEndPoint(jInstance.getUrl(), endPointUrl);
            log.info("getTestStepDetails::: endPoint:" + endPoint);
            String response = rawRestTemplate.exchange(endPoint, HttpMethod.GET,
                    requestBuilder.withAuthHeaderJson(jInstance.getUsername(), jInstance.getPassword()), String.class).getBody();
            log.info("getTestStepDetails::: response:" + response);
            JSONObject jObject = new JSONObject(response);
            JSONArray jsonArray = jObject.getJSONArray("stepBeanCollection");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject teststep = jsonArray.getJSONObject(i);
                Map<String, String> map = new HashMap<>();

                try {
                    String step = teststep.getString("step");
                    map.put("step", step);
                } catch (Exception ex) {

                }
                try {
                    String data = teststep.getString("data");
                    map.put("data", data);
                } catch (Exception ex) {

                }
                try {
                    String result = teststep.getString("result");
                    map.put("result", result);
                } catch (Exception ex) {

                }
                int orderId = teststep.getInt("orderId");
                steps.add(orderId - 1, map);
            }
        } catch (Exception ex) {
            log.info("getTestStepDetails::: Error while getting test step details:", ex);
            throw ex;
        }
        return steps;
    }

    //migrate test Cycle
    public Map<String, String> migrateTestCycleData(String srcInstance, String targetInstance, String projectKey) {
        Map<String, String> result = new TreeMap<>();
        log.info("migrateTestCycleData::: srcInstance: " + srcInstance + " TargetInstance: " + targetInstance + " project : " + projectKey);
        try {
            String srcProjectId = getProjectIdforPKey(srcInstance, projectKey);
            String targetProjectID = getProjectIdforPKey(targetInstance, projectKey);
            Map<String, List<TestCycle>> cycleByVersion = getCycleByVersion(srcInstance, srcProjectId);
            log.info("migrateTestCycleData::: Total project version found is :" + cycleByVersion.size());
            result = createCycleAndExecution(targetInstance, targetProjectID, cycleByVersion);
        } catch (Exception ex) {
            log.error("migrateTestCycleData:::", ex);
        }
        return result;
    }

    public String getProjectIdforPKey(String instance, String projectKey) throws Exception {
        log.info("getProjectIdforPKey::: Instance: " + instance + " project : " + projectKey);
        try {
            String pId;
            JiraInstance jInstance = InstanceHelper.getInstanceDetailsByName(instance, properties);
            String endPointUrl = "/rest/api/2/project/" + projectKey;
            String endPoint = AppUtils.getEndPoint(jInstance.getUrl(), endPointUrl);
            log.info("getProjectIdforPKey::: endPoint:" + endPoint);
            String response = rawRestTemplate.exchange(endPoint, HttpMethod.GET,
                    requestBuilder.withAuthHeaderJson(jInstance.getUsername(), jInstance.getPassword()), String.class).getBody();
            log.info("getProjectIdforPKey::: response:" + response);
            JSONObject jsonObject = new JSONObject(response);
            pId = jsonObject.getString("id");
            return pId;
        } catch (Exception ex) {
            throw ex;
        }

    }

    public Map<String, List<TestCycle>> getCycleByVersion(String instance, String projectId) throws Exception {
        Map<String, List<TestCycle>> versionMap = new HashMap<>();
        try {
            JiraInstance jInstance = InstanceHelper.getInstanceDetailsByName(instance, properties);
            String endPointUrl = "/rest/zapi/latest/cycle?projectId=" + projectId;
            String endPoint = AppUtils.getEndPoint(jInstance.getUrl(), endPointUrl);
            log.info("getCycleByVersion::: endPoint:" + endPoint);
            String response = rawRestTemplate.exchange(endPoint, HttpMethod.GET,
                    requestBuilder.withAuthHeaderJson(jInstance.getUsername(), jInstance.getPassword()), String.class).getBody();
            log.info("getCycleByVersion::: response:" + response);
            JSONObject jsonObject = new JSONObject(response);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                log.info("getCycleByVersion::: Version Id:" + key);
                List<TestCycle> cycleList = new ArrayList<>();
                JSONArray cycles = jsonObject.getJSONArray(key);
                for (int i = 0; i < cycles.length(); i++) {
                    JSONObject object = cycles.getJSONObject(i);
                    Iterator<String> cycleKeys = object.keys();
                    while (cycleKeys.hasNext()) {
                        String cycleKey = cycleKeys.next();
                        log.info("getCycleByVersion::: Cycle Id:" + cycleKey);
                        if (!"recordsCount".equals(cycleKey)) {
                            JSONObject cycleObject = object.getJSONObject(cycleKey);
                            TestCycle cycle = convertToCycleObject(cycleObject, projectId);
                            cycle.setCycleId(cycleKey);
                            List<TestCycleIssue> executionDetail = getExecutionDetails(instance, cycleKey, projectId, key);
                            cycle.setExecution(executionDetail);
                            cycleList.add(cycle);
                        }
                    }
                }
                versionMap.put(key, cycleList);
            }
            return versionMap;
        } catch (Exception ex) {
            log.error("getCycleByVersion:::Error while getting cycle and versions :", ex);
            throw ex;
        }
    }

    private TestCycle convertToCycleObject(JSONObject cycleObject, String projectId) throws JSONException {
        TestCycle cycle = new TestCycle();
        cycle.setVersionName(cycleObject.getString("versionName"));
        cycle.setName(cycleObject.getString("name"));
        try {
            String description = cycleObject.getString("description");
            cycle.setDescription(description);
        } catch (Exception ex) {

        }
        try {
            String build = cycleObject.getString("build");
            cycle.setDescription(build);
        } catch (Exception ex) {

        }
        try {
            String environment = cycleObject.getString("environment");
            cycle.setDescription(environment);
        } catch (Exception ex) {

        }
        try {
            String createdBy = cycleObject.getString("createdBy");
            cycle.setCreatedBy(createdBy);
        } catch (Exception ex) {

        }
        try {
            String createdByDisplay = cycleObject.getString("createdByDisplay");
            cycle.setCreatedByDisplay(createdByDisplay);
        } catch (Exception ex) {

        }
        try {
            String modifiedBy = cycleObject.getString("modifiedBy");
            cycle.setModifiedBy(modifiedBy);
        } catch (Exception ex) {

        }
        try {
            String startDate = cycleObject.getString("startDate");
            cycle.setStartDate(startDate);
        } catch (Exception ex) {

        }
        try {
            String endDate = cycleObject.getString("endDate");
            cycle.setEndDate(endDate);
        } catch (Exception ex) {

        }
        try {
            String ended = cycleObject.getString("ended");
            cycle.setEnded(ended);
        } catch (Exception ex) {

        }
        return cycle;
    }

    private List<TestCycleIssue> getExecutionDetails(String instance, String cycleId, String projectId, String versionId) throws Exception {
        List<TestCycleIssue> execution = null;
        JiraInstance jInstance = InstanceHelper.getInstanceDetailsByName(instance, properties);
        String endPointUrl = "/rest/zapi/latest/execution?action=expand&cycleId=" + cycleId + "&projectId=" + projectId + "&versionId=" + versionId;
        String endPoint = AppUtils.getEndPoint(jInstance.getUrl(), endPointUrl);
        log.info("getExecutionDetails::: endPoint:" + endPoint);
        if (executionDetailsCache.containsKey(endPoint)) {
            log.info("getExecutionDetails::: returning from cache:");
            execution = executionDetailsCache.get(endPoint);
            log.info("getExecutionDetails::: return from cache: Execution :" + execution);
            return execution;
        } else {
            execution = new ArrayList<>();
            String response = rawRestTemplate.exchange(endPoint, HttpMethod.GET,
                    requestBuilder.withAuthHeaderJson(jInstance.getUsername(), jInstance.getPassword()), String.class).getBody();
            log.info("getExecutionDetails::: response:" + response);
            JSONObject responseObject = new JSONObject(response);
            JSONArray jsonArray = responseObject.getJSONArray("executions");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject executions = jsonArray.getJSONObject(i);
                String executionId = executions.getString("id");
                TestCycleIssue tcIssue = convertToCycleIssueObject(executions, projectId);
                execution.add(tcIssue);
            }
            executionDetailsCache.put(endPoint, execution);
        }
        return execution;
    }

    private TestCycleIssue convertToCycleIssueObject(JSONObject cycleObject, String projectId) throws JSONException {
        TestCycleIssue cycleIssue = new TestCycleIssue();
        cycleIssue.setExecutionId(cycleObject.getString("id"));
        cycleIssue.setIssueId(cycleObject.getString("issueId"));
        cycleIssue.setIssueKey(cycleObject.getString("issueKey"));
        cycleIssue.setExecutionStatus(cycleObject.getString("executionStatus"));
        cycleIssue.setOrderId(cycleObject.getInt("orderId"));
        try {
            String executedOn = cycleObject.getString("executedOn");
            cycleIssue.setExecutedOn(executedOn);
        } catch (Exception ex) {

        }
        try {
            String executedBy = cycleObject.getString("executedBy");
            cycleIssue.setExecutedBy(executedBy);
        } catch (Exception ex) {

        }
        try {
            String assignedTo = cycleObject.getString("assignedTo");
            cycleIssue.setAssignedTo(assignedTo);
        } catch (Exception ex) {

        }
        try {
            String assignedToDisplay = cycleObject.getString("assignedToDisplay");
            cycleIssue.setAssignedToDisplay(assignedToDisplay);
        } catch (Exception ex) {

        }
        try {
            String createdBy = cycleObject.getString("createdBy");
            cycleIssue.setCreatedBy(createdBy);
        } catch (Exception ex) {

        }
        try {
            String executedByDisplay = cycleObject.getString("executedByDisplay");
            cycleIssue.setExecutedByDisplay(executedByDisplay);
        } catch (Exception ex) {

        }
        try {
            String modifiedBy = cycleObject.getString("modifiedBy");
            cycleIssue.setModifiedBy(modifiedBy);
        } catch (Exception ex) {

        }
        try {
            String comment = cycleObject.getString("comment");
            cycleIssue.setComment(comment);
        } catch (Exception ex) {

        }

        try {
            JSONArray defArray = cycleObject.getJSONArray("defects");
            List<String> defects = new ArrayList<>();
            for (int i = 0; i < defArray.length(); i++) {
                JSONObject defObject = defArray.getJSONObject(i);
                String defectKey = defObject.getString("key");
                defects.add(defectKey);
            }
            cycleIssue.setDefects(defects);
        } catch (Exception ex) {

        }
        return cycleIssue;
    }

    private Map<String, String> createCycleAndExecution(String instance, String targetProjectId,
                                                        Map<String, List<TestCycle>> srcCylceByVersion) throws Exception {
        log.info("createCycleAndExecution::: Instance: " + instance + " TargetProjectId: " + targetProjectId);
        List<String> cylceCreatedByUpdateQuery = new ArrayList<>();
        List<String> executionCreatedByUpdateQuery = new ArrayList<>();
        Map<String, String> query = new HashMap<>();
        Map<String, String> testCaseUpdateFailed = new HashMap<>();
        Map<String, String> createCycleFailedResult = new HashMap<>();

        for (String key : srcCylceByVersion.keySet()) {
            List<TestCycle> cycles = srcCylceByVersion.get(key);
            Collections.sort(cycles);
            for (TestCycle tc : cycles) {
                String versionName = tc.getVersionName();
                String versionId = "-1";
                if (!versionName.equals("Unscheduled")) {
                    versionId = getVersionId(instance, targetProjectId, versionName);
                    if (versionId == null) {
                        log.error("version name does not exists in target instance" + versionName);
                        throw new Exception("version name does not exists in target instance" + versionName);
                    }
                }
                String cycleName = tc.getName();
                String cycleId = getCycleId(instance, targetProjectId, cycleName, versionId);
                if (cycleId == null) {
                    // create cycle on target as no cycle exist
                    try {
                        cycleId = createCycleonTarget(instance, versionId, targetProjectId, tc, cylceCreatedByUpdateQuery);
                        attachTestCaseToCycle(instance, targetProjectId, versionId, cycleId, tc.getExecution());
                        //update test case and execution status to be done latter
                    } catch (Exception ex) {
                        createCycleFailedResult.put("versionName:" + versionName + ",CycleName:" + cycleName, " Failed to create cycle, please check manually");
                    }
                }
                else{
                    int targetIssueCountInCycle = getIssueCountInCycle(instance,cycleId,targetProjectId,versionId);
                    List<TestCycleIssue> srcExecutions = tc.getExecution();
                    if(targetIssueCountInCycle>0){
                        createCycleFailedResult.put("versionName:" + versionName + ",CycleName:" + cycleName, " Execution details for Cycle allready exists");
                    }
                    else {
                        if(srcExecutions != null && srcExecutions.size()>0){
                            attachTestCaseToCycle(instance, targetProjectId, versionId, cycleId, srcExecutions);
                        }
                    }
                }
            }
        }
        log.info("createCylceAndExecution::: query below for cycle creation");
        StringBuffer buffer = new StringBuffer();
        cylceCreatedByUpdateQuery.forEach(
            cquery -> {
                buffer.append(cquery);
            }
        );
        log.info("createCylceAndExecution::: Final query for cycle creation :" + buffer);
        StringBuffer buffer1 = new StringBuffer();
        executionCreatedByUpdateQuery.forEach(
                equery -> {
                    buffer1.append(equery);
                }
        );
        log.info("createCylceAndExecution::: Final query for execution creation :" + buffer1);
        query.put("Query For Test cycle: " , buffer.toString());
        query.put("Query For Test Execution: " , buffer1.toString());
        query.putAll(createCycleFailedResult);
        query.putAll(testCaseUpdateFailed);
        return query;
    }

    public static Map<String, String> versionIdCache = new HashMap<>();

    private String getVersionId(String instance, String projectId, String versionName) throws Exception {
        String versionId = null;
        try {
            JiraInstance jInstance = InstanceHelper.getInstanceDetailsByName(instance, properties);
            String endPointUrl = "/rest/api/2/project" + projectId + "/versions";
            String endPoint = AppUtils.getEndPoint(jInstance.getUrl(), endPointUrl);
            log.info("getVersionId::: endPoint:" + endPoint);
            if (versionIdCache.containsKey(endPoint)) {
                log.info("getVersionId::: returning from cache:");
                versionId = versionIdCache.get(endPoint);
                log.info("getVersionId::: return from cache: versionid :" + versionId);
                return versionId;
            } else {
                String response = rawRestTemplate.exchange(endPoint, HttpMethod.GET,
                        requestBuilder.withAuthHeaderJson(jInstance.getUsername(), jInstance.getPassword()), String.class).getBody();
                log.info("getExecutionDetails::: response:" + response);
                JSONArray responseArray = new JSONArray(response);
                for (int i = 0; i < responseArray.length(); i++) {
                    JSONObject verObject = responseArray.getJSONObject(i);
                    if (verObject.getString("name").equals(versionName)) {
                        versionId = verObject.getString("id");
                        break;
                    }
                }
                versionIdCache.put(endPoint, versionId);
                log.info("getVersionId::: returns: versionid :" + versionId);
            }
        } catch (Exception ex) {
            log.error("getVersionId::: Errors:", ex);
            throw ex;
        }
        return versionId;
    }

    private String getCycleId(String instance, String projectId, String cycleName, String versionId) throws Exception {
        String cycleId = null;
        try {
            JiraInstance jInstance = InstanceHelper.getInstanceDetailsByName(instance, properties);
            String endPointUrl = "/rest/zapi/latest/cycle?projectId=" + projectId + "&versionId=" + versionId;
            String endPoint = AppUtils.getEndPoint(jInstance.getUrl(), endPointUrl);
            log.info("getCycleId::: endPoint:" + endPoint);
            String response = rawRestTemplate.exchange(endPoint, HttpMethod.GET,
                    requestBuilder.withAuthHeaderJson(jInstance.getUsername(), jInstance.getPassword()), String.class).getBody();
            log.info("getCycleId::: response:" + response);
            JSONObject cycleObject = new JSONObject(response);
            Iterator<String> keys = cycleObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!"recordCount".equals(key)) {
                    JSONObject obj = cycleObject.getJSONObject(key);
                    if (obj.getString("name").equals(cycleName)) {
                        cycleId = obj.getString("id");
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            log.error("getVersionId::: Errors:", ex);
            throw ex;
        }
        return cycleId;
    }

    private String createCycleonTarget(String instance, String versionId,
                                       String projectId, TestCycle srcCycle, List<String> cycleCreatedByUpdateQuery) throws Exception {
        log.info("createCycleonTarget::: instance:" + instance + "  projectId :" + projectId
                + "  versionId :" + versionId + "  cycleName :" + srcCycle.getName());
        String cycleId = null;
        try {
            JiraInstance jInstance = InstanceHelper.getInstanceDetailsByName(instance, properties);
            String endPointUrl = "/rest/zapi/latest/cycle";
            String endPoint = AppUtils.getEndPoint(jInstance.getUrl(), endPointUrl);
            log.info("createCycleonTarget::: endPoint:" + endPoint);
            Map payload = new HashMap<>();
            payload.put("name", srcCycle.getName());
            payload.put("projectId", projectId);
            payload.put("versionId", versionId);

            String build = srcCycle.getBuild();
            if (Objects.nonNull(build)) {
                payload.put("build", build);
            }
            String environment = srcCycle.getEnvironment();
            if (Objects.nonNull(environment)) {
                payload.put("environment", environment);
            }
            String description = srcCycle.getDescription();
            if (Objects.nonNull(description)) {
                payload.put("description", description);
            }
            String StartDate = srcCycle.getStartDate();
            if (Objects.nonNull(StartDate)) {
                payload.put("startDate", StartDate);
            }
            String endDate = srcCycle.getEndDate();
            if (Objects.nonNull(StartDate)) {
                payload.put("endDate", endDate);
            }
            String jsonString = objectMapper.writeValueAsString(payload);
            log.info("createCycleonTarget::: request:" + jsonString);
            String response = rawRestTemplate.exchange(endPoint, HttpMethod.POST,
                    requestBuilder.withAuthHeaderandBody(jInstance.getUsername(), jInstance.getPassword(), jsonString), String.class).getBody();
            log.info("createCycleonTarget::: response:" + response);
            JSONObject cycleResponse = new JSONObject(response);
            cycleId = cycleResponse.getString("id");
            String query = "Update AO_7DEABF_Cycle set created_by='" + srcCycle.getCreatedBy() + "', Modified_By='" + srcCycle.getModifiedBy()
                    + "' where id=" + cycleId + " and project_id =" + projectId + " and version_id=" + versionId;
            log.info("createCycleonTarget:::query: " + query);
            cycleCreatedByUpdateQuery.add(query);

        } catch (Exception ex) {
            log.error("createCycleonTarget::: Error while creating cycle on target:", ex);
            throw ex;
        }
        return cycleId;
    }

    private void attachTestCaseToCycle(String instance, String projectId, String versionId, String cycleId, List<TestCycleIssue> executions)
            throws Exception {
        log.info("attachTestCaseToCycle::: instance:" + instance + "  projectId :" + projectId
                + "  versionId :" + versionId + "  execution count :" + executions.size());
        try {
            Collections.sort(executions);
            List<String> issueKeys = executions.stream().map(testCycleIssue -> testCycleIssue.getIssueKey()).collect(Collectors.toList());
            if (issueKeys.size() > 0) {
                Map payload = new HashMap<>();
                payload.put("method", "1");
                payload.put("projectId", projectId);
                payload.put("versionId", versionId);
                //payload.put("versionId", versionId);
                payload.put("issues", issueKeys);
                JiraInstance jInstance = InstanceHelper.getInstanceDetailsByName(instance, properties);
                String endPointUrl = "/rest/zapi/latest/execution/addTeststoCycle/";
                String endPoint = AppUtils.getEndPoint(jInstance.getUrl(), endPointUrl);
                log.info("attachTestCaseToCycle::: endPoint:" + endPoint);
                String jsonString = objectMapper.writeValueAsString(payload);
                log.info("attachTestCaseToCycle::: request:" + jsonString);
                String response = rawRestTemplate.exchange(endPoint, HttpMethod.POST,
                        requestBuilder.withAuthHeaderandBody(jInstance.getUsername(), jInstance.getPassword(), jsonString), String.class).getBody();
                log.info("attachTestCaseToCycle::: response:" + response);
                //JSONObject cycleResponse = new JSONObject(response);
                // check for job progress token and wait for it
                try {
                    String jobProgressToken = new JSONObject(response).getString("jobProgressToken");
                    log.error("attachTestCaseToCycle::: wait for cycle to be created");
                    waitAsyncJobToBeCompleted(instance, jobProgressToken);
                } catch (Exception ex) {

                }
            }
        } catch (Exception ex) {
            log.error("attachTestCaseToCycle::: Error while creating cycle on target:", ex);
            throw ex;
        }
    }

    private void waitAsyncJobToBeCompleted(String instance, String jobProgressToken) {
        double jobProgress;
        do {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                log.error("attachTestCaseToCycle::: Job progress: Error while try to make the thread sleeping 500ms. " + e.getLocalizedMessage());
                e.printStackTrace();
            }
            jobProgress = getJobProgress(instance, jobProgressToken);
        } while (Double.compare(jobProgress, 1) < 0);
    }

    private double getJobProgress(String instance, String jobProgressToken) {

        String zapiResponse = null;
        try {
            JiraInstance jInstance = InstanceHelper.getInstanceDetailsByName(instance, properties);
            String endPointUrl = "/rest/zapi/latest/execution/jobProgress/" + jobProgressToken;
            String endPoint = AppUtils.getEndPoint(jInstance.getUrl(), endPointUrl);
            log.info("attachTestCaseToCycle::: endPoint:" + endPoint);
            zapiResponse = rawRestTemplate.exchange(endPoint, HttpMethod.GET,
                    requestBuilder.withAuthHeaderJson(jInstance.getUsername(), jInstance.getPassword()), String.class).getBody();
            log.trace("attachTestCaseToCycle::: Job progress: " + zapiResponse);
            double progress = new JSONObject(zapiResponse).getDouble("progress");
            log.debug("attachTestCaseToCycle::: Job progress: " + progress);
            return progress;


        } catch (JSONException e) {
            String err = String.format("Error while retrieving the job progress from JSON: %s\n%s",
                    zapiResponse, e.getLocalizedMessage());
            log.error(err);
            //throw e;
            //throw new RestApiException(err, e);
            return 1;
        }

    }

    private int getIssueCountInCycle(String instance, String cycleId, String projectId, String versionId) throws Exception {
        try {
            JiraInstance jInstance = InstanceHelper.getInstanceDetailsByName(instance, properties);
            String endPointUrl = "/rest/zapi/latest/execution?action=expand&cycleId=" + cycleId + "&projectId=" + projectId + "&versionId=" + versionId;
            String endPoint = AppUtils.getEndPoint(jInstance.getUrl(), endPointUrl);
            log.info("getIssueCountInCycle::: endPoint:" + endPoint);
            String response = rawRestTemplate.exchange(endPoint, HttpMethod.GET,
                    requestBuilder.withAuthHeaderJson(jInstance.getUsername(), jInstance.getPassword()), String.class).getBody();
            log.info("getIssueCountInCycle::: response:" + response);
            JSONObject object = new JSONObject(response);
            JSONArray exeArray = object.getJSONArray("executions");
            return exeArray.length();
        } catch (Exception ex) {
            log.error("getIssueCountInCycle::: Errors:", ex);
            throw ex;
        }
    }
}
