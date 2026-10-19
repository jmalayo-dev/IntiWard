package pe.intiguard.comprobantes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ComprobantesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComprobantesApplication.class, args);
    }
}
