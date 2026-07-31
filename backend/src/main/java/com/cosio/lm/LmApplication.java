package com.cosio.lm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import com.pgvector.PGvector;

import io.github.bucket4j.Bucket;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@SpringBootApplication
@EnableScheduling
public class LmApplication {

	public static void main(String[] args) {
		SpringApplication.run(LmApplication.class, args);
	}

    @Bean
    public ConcurrentHashMap<String, Bucket> createBucketMap() {
        return new ConcurrentHashMap<String, Bucket>();
    }

    /**
     * Bean handles limiting per-user traffic to the endpoints 
     * Controller#embedDocument and Controller#generate using
     * RateLimiter.
     * 
     * @param interceptor is the object handling rate limiting
     * @return a configurer
     */
    @Bean
    public WebMvcConfigurer webMvcConfigurer(RateLimiter interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor).addPathPatterns("/generate", "/embedDocument");
            } 
        };
    }

	/**
	 * WebClient to access microservices
	 * @return
	 */
	@Bean
    WebClient llmWebClient() {
        return WebClient.create("http://localhost:8000");
    }

	/**
	 * For proper JDBC compatability with pgvector
     * 
	 * @param dataSource
	 * @return
	 * @throws Exception
	 */
    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        DataSource original = properties.initializeDataSourceBuilder().build();
        return new VectorDataSource(original);
    }

	/**
	 * Proper CORS configuration to allow cross-host from frontend
	 * @return
	 */
	@Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
	
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    static class VectorDataSource extends DelegatingDataSource {
        public VectorDataSource(DataSource targDataSource) {
            super(targDataSource);
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection conn = super.getConnection();
            PGvector.addVectorType(conn);
            return conn;
        }

    }

}
