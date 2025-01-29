package org.openmrs.module.ehospitalws.config;

import org.openmrs.api.context.Context;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class DatabaseConfig {
	
	@Bean
	public DataSource dataSource() {
		// Fetch properties from OpenMRS runtime properties
		Properties properties = Context.getRuntimeProperties();
		
		String driverClassName = getProperty(properties, "connection.driver_class");
		String url = getProperty(properties, "connection.url");
		String username = getProperty(properties, "connection.username");
		String password = getProperty(properties, "connection.password");
		
		// Set up the data source
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(driverClassName);
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		
		return dataSource;
	}
	
	@Bean
	public JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}
	
	// Helper method to fetch and trim properties
	public static String getProperty(Properties properties, String key) {
		String value = properties.getProperty(key, "").trim();
		if (value.isEmpty()) {
			throw new IllegalStateException("Missing required OpenMRS property: " + key);
		}
		return value;
	}
}
