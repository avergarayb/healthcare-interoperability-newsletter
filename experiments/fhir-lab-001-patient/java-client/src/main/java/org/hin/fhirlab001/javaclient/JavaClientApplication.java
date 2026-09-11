package org.hin.fhirlab001.javaclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FhirProperties.class)
public class JavaClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaClientApplication.class, args);
    }
}
