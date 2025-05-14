package online.bmj.www;
    
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// @EnableCaching
@EnableScheduling
public class bmjServer {
    
    public static void main(String[] args) {
        SpringApplication.run(bmjServer.class, args);
    }
}