package rta.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 所有 API
                        .allowedOrigins("http://localhost:4200") // 允許前端網址
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允許的方法
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
