package com.cosmic.scavengers.core.db;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class JsonToMapConverter implements AttributeConverter<Map<String, Object>, String> {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(Map<String, Object> attribute) {
		if (attribute == null)
			return "{}";
		try {
			return objectMapper.writeValueAsString(attribute);
		} catch (JsonProcessingException e) {
			// Using a generic RuntimeException for now
			throw new RuntimeException("Error serializing Map to JSON", e);
		}
	}

	@Override
	public Map<String, Object> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isEmpty())
			return new HashMap<>();
		try {
			return objectMapper.readValue(dbData, new TypeReference<Map<String, Object>>() {
			});
		} catch (IOException e) {
			throw new RuntimeException("Error deserializing JSON to Map", e);
		}
	}
}