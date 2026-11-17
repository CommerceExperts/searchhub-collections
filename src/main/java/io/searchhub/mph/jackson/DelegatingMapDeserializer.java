package io.searchhub.mph.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Map;

public class DelegatingMapDeserializer extends StdDeserializer<Map<?, ?>> implements ContextualDeserializer {

	private final JsonDeserializer<?> defaultDeserializer;

	public DelegatingMapDeserializer(JsonDeserializer<?> defaultDeserializer) {
		super(Map.class);
		this.defaultDeserializer = defaultDeserializer;
	}

	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
		// resolve the default Jackson map deserializer (handles key/value generics for standard maps)
		JsonDeserializer<?> delegate = defaultDeserializer;
		if (defaultDeserializer instanceof ContextualDeserializer) {
			delegate = ((ContextualDeserializer) defaultDeserializer).createContextual(ctxt, property);
		}

		// If the field has custom annotation, activate the custom MPH deserialization
		if (property != null && property.getAnnotation(UseMPHMapPolymorphism.class) != null) {
			return new MPHAnnotatedMapDeserializer(delegate, property.getType());
		}

		return delegate;
	}

	@Override
	public Map<?, ?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		return (Map<?, ?>) defaultDeserializer.deserialize(p, ctxt);
	}
}
