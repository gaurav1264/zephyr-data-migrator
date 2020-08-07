package com.prestige.zephyr.migrator.Domain;

import lombok.Data;

import java.util.List;

@Data
public class StepResult {
    private String id;
    private String status;
    private String comment;
    private String executionId;
    private String stepId;

    private String executedBy;

    private String executedOn;


    private String createdBy;
    private String modifiedBy;
    private String assignedTo;

    private String testStep;
    private String testData;
    private String expectedResult;
    private String orderId;
    private List<String> defects;
}
