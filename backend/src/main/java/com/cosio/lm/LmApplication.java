package com.cosio.lm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

import javax.sql.DataSource;
import java.sql.Connection;
import com.pgvector.PGvector;

import java.util.List;

import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@SpringBootApplication
public class LmApplication {

	public static void main(String[] args) {
		SpringApplication.run(LmApplication.class, args);
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
	 * @param dataSource
	 * @return
	 * @throws Exception
	 */
	@Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) throws Exception {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.execute((Connection conn) -> {
            PGvector.addVectorType(conn);
            return null;
        });
        return template;
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
	

}
