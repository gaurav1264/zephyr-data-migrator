package com.prestige.zephyr.migrator.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "jira")
public class JiraProperties implements GlobalServiceInstanceProperties {

    @Getter
    @Setter
    private List<JiraInstance> instances = new ArrayList<JiraInstance>();

    @Override
    public List<ServerInstance> instances(){
        List<ServerInstance> instances = new ArrayList<ServerInstance>();
        for(JiraInstance instance : this.instances){
            instances.add(instance);
        }
        return  instances;
    }

}
