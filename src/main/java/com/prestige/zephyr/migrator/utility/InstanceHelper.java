package com.prestige.zephyr.migrator.utility;

import com.prestige.zephyr.migrator.domain.JiraInstance;
import com.prestige.zephyr.migrator.domain.JiraProperties;

public class InstanceHelper {
    public static JiraInstance getInstancedetailsByName(String instance, JiraProperties properties){
        for(JiraInstance instance1:properties.getInstances()){
            if(instance1.getName().equalsIgnoreCase(instance)){
                JiraInstance jiraInstance = new JiraInstance();
                jiraInstance.setName(instance1.getName());
                jiraInstance.setUrl(instance1.getUrl());
                jiraInstance.setUsername(instance1.getUsername());
                jiraInstance.setPassword(instance1.getPassword());
                return jiraInstance;
            }
        }
        return null;
    }
}
