package com.cosmic.scavengers.networking.mappers;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.networking.proto.traits.TraitInstanceProto;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;

@Component
public class TraitProtobufMapper {
	private static final Logger log = LoggerFactory.getLogger(TraitProtobufMapper.class);

	private static final String PROTO_PACKAGE = "com.cosmic.scavengers.networking.proto.traits.";
	private static final String CLASS_SUFFIX = "TraitProto";
	
	private final Map<String, Class<? extends Message>> classCache = new ConcurrentHashMap<>();
	private final ObjectMapper mapper;

	public TraitProtobufMapper(ObjectMapper mapper) {
		// Mapper had to be explicitly set (overridden), as the Spring Component does
		// not correctly inject the one defined in global configuration class:
		// "com.cosmic.scavengers.config.JacksonConfig".
		// They are identical, but in this way the correct one is always used.
		this.mapper = mapper
				.copy()
				.registerModule(new ProtobufModule())
				.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@SuppressWarnings("unchecked")
	public Optional<TraitInstanceProto> mapToProto(String traitId, Map<String, Object> data) {
		if (traitId == null) {
			log.error("Trait Mapping Error: TraitId is null. Skipping.");
			return Optional.empty();
		}

		try {
			Class<? extends Message> clazz = classCache.computeIfAbsent(traitId.toLowerCase(), id -> {
				try {
					String className = PROTO_PACKAGE + toPascalCase(id) + CLASS_SUFFIX;
					return (Class<? extends Message>) Class.forName(className);
				} catch (ClassNotFoundException e) {
					log.error("Trait Mapping Error: No Proto class found for TraitId: '{}'", id);
					return null;
				}
			});

			if (clazz == null) {
				log.error("Trait Mapping Error: No Proto class found for TraitId: '{}'", traitId);
				return Optional.empty();
			}

			String json = mapper.writeValueAsString(data);
			Message traitMessage = mapper.readValue(json, clazz);
			
			if (log.isTraceEnabled()) {
				String shortData = TextFormat.printer()
			            .emittingSingleLine(true)
			            .printToString(traitMessage);
	            
			    log.trace("Trait Mapped: [{}] | Data: [{}]", traitId, shortData);
			}

			Any packedData = Any.pack(traitMessage);
			return Optional.of(TraitInstanceProto.newBuilder()
					.setTraitId(traitId.toLowerCase())
					.setData(packedData).build());

		} catch (Exception e) {
			log.error("Trait Mapping Error: Failed to map Trait '{}'.", traitId, e);
			return Optional.empty();
		}
	}

	/**
	 * Converts snake_case (e.g. "mining_laser") to PascalCase (e.g. "MiningLaser")
	 */
	private String toPascalCase(String snakeStr) {
		if (snakeStr == null || snakeStr.isEmpty())
			return snakeStr;

		StringBuilder result = new StringBuilder();
		for (String part : snakeStr.split("_")) {
			if (!part.isEmpty()) {
				result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
			}
		}
		return result.toString();
	}
}
