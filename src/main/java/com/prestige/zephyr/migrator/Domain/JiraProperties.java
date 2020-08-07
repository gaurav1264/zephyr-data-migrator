package com.prestige.zephyr.migrator.Domain;

//import jdk.nashorn.internal.objects.annotations.Getter;
//import jdk.nashorn.internal.objects.annotations.Setter;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "properties")
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
