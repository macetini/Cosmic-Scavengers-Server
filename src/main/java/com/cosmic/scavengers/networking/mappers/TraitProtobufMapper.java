package com.cosmic.scavengers.networking.mappers;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.networking.proto.traits.TraitInstanceProto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

/**
 * Authoritative mapper that dynamically converts raw Trait Maps into Protobuf
 * Any containers. Uses naming conventions to resolve Protobuf classes via
 * reflection, eliminating the need for switch statements or manual
 * registration.
 */
@Component
public class TraitProtobufMapper {
	private static final Logger log = LoggerFactory.getLogger(TraitProtobufMapper.class);

	private static final String PROTO_PACKAGE = "com.cosmic.scavengers.networking.proto.traits.";
	private static final String CLASS_SUFFIX = "TraitProto";

	private final ObjectMapper mapper;

	public TraitProtobufMapper(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * Dynamically maps a trait ID and its data to a TraitInstanceProto. * @param
	 * traitId The ID (e.g., "movable") which maps to class "MovableTraitProto"
	 * 
	 * @param data The properties to inject into the Protobuf message
	 * @return An Optional containing the packed trait, or empty if mapping failed
	 */
	public Optional<TraitInstanceProto> mapToProto(String traitId, Map<String, Object> data) {
		if (traitId == null) {
			log.warn("Trait ID cannot be null for mapping.");
			return Optional.empty();
		}

		try {
			String className = PROTO_PACKAGE + capitalize(traitId) + CLASS_SUFFIX;
			Class<?> clazz = Class.forName(className);

			Method newBuilderMethod = clazz.getMethod("newBuilder");
			Message.Builder traitBuilder = (Message.Builder) newBuilderMethod.invoke(null);

			mapper.updateValue(traitBuilder, data);

			Message traitMessage = traitBuilder.build();
			Any packedData = Any.pack(traitMessage);

			TraitInstanceProto instance = TraitInstanceProto.newBuilder()
					.setTraitId(traitId.toLowerCase())
					.setData(packedData)
					.build();

			return Optional.of(instance);
		} catch (ClassNotFoundException e) {
			log.error("Trait Mapping Error: No Proto class found for TraitId: '{}' (Expected: {}{}{})",
					traitId, PROTO_PACKAGE, capitalize(traitId), CLASS_SUFFIX);
		} catch (Exception e) {
			log.error("Trait Mapping Error: Failed to dynamically map trait '{}'. Check for property mismatches.", traitId, e);
		}
		
		log.warn("Trait Mapping Error: Failed to dynamically map trait '{}'.", traitId);
		return Optional.empty();
	}

	/**
	 * Helper to ensure to match Java class naming.
	 */
	private String capitalize(String str) {
		if (str == null || str.isEmpty()) {
			log.warn("Attempted to capitalize a null or empty string.");
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
	}
}
