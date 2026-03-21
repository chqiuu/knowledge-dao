package com.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.net.InetAddress;

@SpringBootApplication
public class KnowledgeDaoApplication {

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext ctx = SpringApplication.run(KnowledgeDaoApplication.class, args);
        Environment env = ctx.getEnvironment();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port", "8080");
        String path = env.getProperty("server.servlet.context-path", "");
        String portPath = ":" + port + path;
        String delimiter = "=".repeat(70);
        System.out.println("\n" + delimiter);
        System.out.println("【Knowledge DAO】项目已启动");
        System.out.println("访问地址:");
        System.out.println("  Local:    http://localhost" + portPath);
        System.out.println("  External: http://" + ip + portPath);
        System.out.println("  API文档:  http://localhost" + portPath + "/doc.html");
        System.out.println(delimiter);
    }
}
