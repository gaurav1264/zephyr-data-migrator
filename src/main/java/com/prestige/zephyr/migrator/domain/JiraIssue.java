package com.prestige.zephyr.migrator.domain;

import lombok.Data;

@Data
public class JiraIssue {
    private String issueKey;
    private Long oldIssueId;
    private Long newIssueId;
}
