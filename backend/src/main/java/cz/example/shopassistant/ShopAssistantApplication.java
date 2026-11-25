package cz.example.shopassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.modulith.Modulithic;

@Modulithic
@SpringBootApplication
@EnableScheduling
public class ShopAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopAssistantApplication.class, args);
    }
}
