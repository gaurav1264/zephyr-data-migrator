package com.prestige.zephyr.migrator.domain;

import lombok.Data;

import java.util.List;

@Data
public class TestCycleIssue implements Comparable<TestCycleIssue> {
    private String issueId;
    private String issueKey;
    private String issueSummary;
    private String issueDescription;

    private Integer orderId;
    private String executionId;
    private String executionStatus;

    private String executedBy;
    private String executedByDisplay;
    private String executedOn;

    private String comment;

    private String createdBy;
    private String modifiedBy;
    private String assignedTo;
    private String assignedToDisplay;
    private List<String> defects;
    private List<StepResult> stepResults;

    @Override
    public int compareTo(TestCycleIssue arg0) {
        return this.getOrderId().compareTo(arg0.getOrderId());
    }
}
