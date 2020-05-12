package com.danawa.fastcatx.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;

@SpringBootApplication(exclude = {MultipartAutoConfiguration.class})
public class FastcatxServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FastcatxServerApplication.class, args);
    }

}
