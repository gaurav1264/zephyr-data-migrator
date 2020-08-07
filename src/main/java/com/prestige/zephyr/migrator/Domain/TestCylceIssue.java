package com.prestige.zephyr.migrator.Domain;

import lombok.Data;

import java.util.List;

@Data
public class TestCylceIssue implements Comparable<TestCylceIssue> {
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
    private List<String> defects;
    private List<StepResult> stepResults;

    @Override
    public int compareTo(TestCylceIssue arg0) {
        return this.getOrderId().compareTo(arg0.getOrderId());
    }
}
