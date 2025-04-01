package kr.co.craftverse.craftverse_blog_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class CraftverseBlogApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CraftverseBlogApiApplication.class, args);
	}

}
