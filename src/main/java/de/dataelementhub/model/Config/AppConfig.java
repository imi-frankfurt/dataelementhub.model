package de.dataelementhub.model.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration // Marks this class as a Spring configuration class
public class AppConfig {

    // Creates a RestTemplate bean that can be injected anywhere in the application
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
