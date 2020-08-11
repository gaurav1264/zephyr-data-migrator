package com.prestige.zephyr.migrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prestige.zephyr.migrator.domain.JiraInstance;
import com.prestige.zephyr.migrator.domain.JiraIssue;
import com.prestige.zephyr.migrator.domain.JiraProperties;
import com.prestige.zephyr.migrator.utility.AppUtils;
import com.prestige.zephyr.migrator.utility.InstanceHelper;
import com.prestige.zephyr.migrator.utility.RequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.codehaus.jettison.json.*;

import java.text.Normalizer;
import java.util.*;


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
            JiraInstance jInstance = InstanceHelper.getInstancedetailsByName(instance, properties);
            String endPointUrl = "/rest/api/2/search?jql=" + projectKey + " AND issuetype=TEST ORDER BY key&maxResults=0";
            String endPoint = AppUtils.getEndPoint(jInstance.getUrl(), endPointUrl);
            log.info("getIssuesByProjectKey::: endPoint:" + endPoint);
            String response = rawRestTemplate.exchange(endPoint, HttpMethod.GET,
                    requestBuilder.withAuthHeader(jInstance.getUsername(), jInstance.getPassword()), String.class).getBody();
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
            JiraInstance jInstance = InstanceHelper.getInstancedetailsByName(instance, properties);
            String endPointUrl = "/rest/api/2/search?jql=" + projectKey + " AND issuetype=TEST ORDER BY key&fields=*none&maxResults=" + maxResult + "&startAt=" + start;
            String endPoint = AppUtils.getEndPoint(jInstance.getUrl(), endPointUrl);
            log.info("getIssuesByJQL::: endPoint:" + endPoint);
            String response = rawRestTemplate.exchange(endPoint, HttpMethod.GET,
                    requestBuilder.withAuthHeader(jInstance.getUsername(), jInstance.getPassword()), String.class).getBody();
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
                    List<Map<String, String>> targetSteps = getTestStepDetails(sourceInstance, issue.getIssueKey(), issue.getOldIssueId());
                    try {
                        if (targetSteps.size() > 0) {
                            log.info("migrateTestStepData::: Test Step Data already found in target issue" + issue.getIssueKey());
                            result.put(issue.getIssueKey(), "Test Step Data already found in target issue");
                        } else {
                            createStepsinTargetIssue(targetInstance,issue,sourceSteps);
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
            JiraInstance jInstance = InstanceHelper.getInstancedetailsByName(instance, properties);
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
            JiraInstance jInstance = InstanceHelper.getInstancedetailsByName(instance, properties);
            String endPointUrl = "/rest/zapi/latest/teststep/" + issueId;
            String endPoint = AppUtils.getEndPoint(jInstance.getUrl(), endPointUrl);
            log.info("getTestStepDetails::: endPoint:" + endPoint);
            String response = rawRestTemplate.exchange(endPoint, HttpMethod.GET,
                    requestBuilder.withAuthHeader(jInstance.getUsername(), jInstance.getPassword()), String.class).getBody();
            log.info("getTestStepDetails::: response:" + endPoint);
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
}
