package ilp.ilp_cw.ilp_1_2;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class IlpConfig {

    public static String getCurrentIlpEndpoint() {
        String envValue = System.getenv("ILP_ENDPOINT");
        String base = (envValue == null || envValue.isBlank())
                ? "https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net"
                : envValue;
        return base.endsWith("/") ? base : base + "/";
    }

    @Bean
    public String ilpEndpoint() {
        return getCurrentIlpEndpoint();
    }

    @Bean(name = "ilpRestTemplate")
    @Primary
    public RestTemplate ilpRestTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(5_000);
                    factory.setReadTimeout(10_000);
                    return factory;
                })
                .build();
    }
//
//    @Bean
//    public ObjectMapper objectMapper() {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new JavaTimeModule());
//        return mapper;
//    }

}
