package com.b4rrhh.workforceloader;

import com.b4rrhh.workforceloader.infrastructure.config.LoaderProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LoaderProperties.class)
public class WorkforceLoaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkforceLoaderApplication.class, args);
    }
}
