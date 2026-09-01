package com.example.baseballorders.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** baseball-orders backendのSpring Bootエントリポイント。 */
@SpringBootApplication
public class BackendApplication {

    /**
     * backendをSpring Bootアプリケーションとして起動する。
     *
     * @param args 起動引数
     */
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
