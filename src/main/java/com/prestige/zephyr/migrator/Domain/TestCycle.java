package com.prestige.zephyr.migrator.Domain;

import com.sun.org.apache.xerces.internal.impl.xpath.XPath;
import lombok.Data;

import java.util.List;

@Data
public class TestCycle implements Comparable<TestCycle>{
    private String versionName;
    private String cycleId;
    private String name;
    private String description;

    private String environment;
    private String build;
    private String createdBy;
    private String modifiedBy;
    private String createdByDisplay;

    private String startDate;
    private String endDate;
    private String ended;

    private List<TestCylceIssue> execution;

    @Override
    public  int compareTo(TestCycle arg0){
        return new Integer(this.getCycleId()).compareTo(new Integer(arg0.getCycleId()));
    }

}
