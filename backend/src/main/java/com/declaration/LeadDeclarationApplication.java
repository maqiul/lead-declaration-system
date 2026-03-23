package com.declaration;

import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 线索申报系统启动类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@Import(ProcessEngineAutoConfiguration.class)
public class LeadDeclarationApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeadDeclarationApplication.class, args);
        System.out.println("==========================================");
        System.out.println("    海关申报系统启动成功！");
        System.out.println("    API文档地址：http://localhost:8080/doc.html");
        System.out.println("==========================================");
    }
}