package com.prestige.zephyr.migrator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@EnableWebMvc
public class AppConfigurations {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.prestige.zephyr.migrator.controller"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(metaData());
    }
    private ApiInfo metaData() {
        ApiInfo apiInfo = new ApiInfo(
                "Zephyr Data Migrator",
                "This is self-service utility to migrate zephyr for Jira data from one instance to another.",
                "1.0.0",
                "Terms of service",
                new Contact("Prestige Inc", "https://gauravbidani.wordpress.com", "gagaurav336@gmail.com"),
                "GPL-3.0 License",
                "https://github.com/gaurav1264/zephyr-data-migrator/blob/master/LICENSE", null);
        return apiInfo;
    }

}
