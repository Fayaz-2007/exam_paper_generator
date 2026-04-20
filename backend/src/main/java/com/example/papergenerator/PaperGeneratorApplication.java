package com.example.papergenerator;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaperGeneratorApplication {

    public static void main(String[] args) {
        // Load .env into System properties before Spring context starts
        try {
            Dotenv dotenv = Dotenv.configure().directory("./").ignoreIfMissing().load();
            dotenv.entries().forEach(e -> {
                if (System.getProperty(e.getKey()) == null) {
                    System.setProperty(e.getKey(), e.getValue());
                }
            });
        } catch (Exception e) {
            System.err.println("Warning: could not load .env — " + e.getMessage());
        }
        SpringApplication.run(PaperGeneratorApplication.class, args);
    }
}
