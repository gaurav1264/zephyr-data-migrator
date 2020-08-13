# Zephyr-data-migrator
This is self-service utility to migrate "Zephyr for Jira" data from one Jira server to another.

# Prerequisites
- Should have "Zephyr for Jira" plugin installed in both source and target Jira server
- Should have Zapi plugin installed in both source and target Jira server
- Should have both Jira up and running
- Should use Jira administration credentials

# Compatibility

# Installation guide
- Setup Java 1.8+
- Downlod the utility jar
- Go to $JAVA_HOME/bin/java -jar 

# Development setup

## Components
- [Java 1.8+](https://adoptopenjdk.net/?variant=openjdk8&jvmVariant=hotspot)
- Spring Boot 2.3+
- Spring framework 5.2+
- Swagger 3.0+
- Lombok

## Instructions
- Clone project locally
- Open project in IntelliJ 2020+
- Enable Annotation processor in IntelliJ
- Open Terminal & Build project using below command
    - "gradlew clean build"
- Run application command
    - gradlew bootRun --args='--spring.profiles.active=dev'
- Once application is running, use below URl to access APIs
    - http://localhost:8080/swagger-ui/index.html

##Rest Endpoints
- migrateteststep : migrate test steps from source instance to destination instance
     - Due to some special characters in the data other than which are handled by the code few test steps for few issues may not be migrated. You will get the list in the output of such data, you can migrate them manually. 
- migratetestcycle: create test Cylces and attach test case to cycles by creating execution on the destination instances.
     - The api has certain limitations, so we provide SQL query for updating created by, modified by, executed by for the cycles and executions as the result of this API.