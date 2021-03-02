package com.prestige.zephyr.migrator.utility;

import com.prestige.zephyr.migrator.domain.JiraInstance;
import com.prestige.zephyr.migrator.domain.JiraProperties;

import java.util.Objects;

public class InstanceHelper {

    public static JiraInstance getInstanceDetailsByName(String instance, JiraProperties properties){
        JiraInstance jiraInstance = null;
        for(JiraInstance propertyInstance : properties.getInstances()) {
            if (propertyInstance.getName().equalsIgnoreCase(instance)) {
                jiraInstance = propertyInstance;
            }
        }
        if(Objects.isNull(jiraInstance)){
            throw new IllegalArgumentException (instance+ " instance is not registered in application.yml. Please use correct instance name");
        }
        return jiraInstance;
    }
}
