package com.cosmic.scavengers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;

@Configuration
public class JacksonConfig {

	@Bean
	@Primary
	ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
				
		mapper.registerModule(new ProtobufModule());
		
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper;
	}
}
