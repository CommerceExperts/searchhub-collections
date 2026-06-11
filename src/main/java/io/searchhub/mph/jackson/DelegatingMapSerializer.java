package io.searchhub.mph.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import io.searchhub.mph.MPHStringMap;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
public class DelegatingMapSerializer extends JsonSerializer<Map<?, ?>> implements ContextualSerializer {

	private final JsonSerializer<?> defaultSerializer;

	@Override
	public void serialize(Map<?, ?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if (value instanceof MPHStringMap) {
			// Route to custom serializer
			JsonSerializer<Object> mphSer = serializers.findValueSerializer(MPHStringMap.class);
			mphSer.serialize(value, gen, serializers);
		}
		else {
			// Route standard HashMaps back to Jackson's highly optimized MapSerializer
			((JsonSerializer<Object>) defaultSerializer).serialize(value, gen, serializers);
		}
	}

	@Override
	public void serializeWithType(Map<?, ?> value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
		if (value instanceof MPHStringMap) {
			JsonSerializer<Object> mphSer = serializers.findValueSerializer(MPHStringMap.class);
			mphSer.serializeWithType(value, gen, serializers, typeSer);
		}
		else {
			((JsonSerializer<Object>) defaultSerializer).serializeWithType(value, gen, serializers, typeSer);
		}
	}

	@Override
	public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
		JsonSerializer<?> resolvedDelegate = defaultSerializer;

		if (defaultSerializer instanceof ContextualSerializer) {
			resolvedDelegate = ((ContextualSerializer) defaultSerializer).createContextual(prov, property);
		}

		// If the delegate didn't change, return this instance. Otherwise, wrap the newly configured delegate.
		return resolvedDelegate == defaultSerializer ? this : new DelegatingMapSerializer(resolvedDelegate);
	}
}
