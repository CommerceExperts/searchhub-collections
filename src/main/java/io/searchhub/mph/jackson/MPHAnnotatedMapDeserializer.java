package io.searchhub.mph.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.searchhub.mph.MPHStringMap;

import java.io.IOException;
import java.util.Map;

class MPHAnnotatedMapDeserializer extends StdDeserializer<Map<?, ?>> {

	private final JsonDeserializer<?> defaultDeserializer;
	private final JavaType            valueType;

	public MPHAnnotatedMapDeserializer(JsonDeserializer<?> defaultDeserializer, JavaType mapType) {
		super(mapType);
		this.defaultDeserializer = defaultDeserializer;
		// Capture the 'V' generic from Map<String, V>
		this.valueType = mapType.getContentType();
	}

	@Override
	public Map<?, ?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		JsonNode node = p.getCodec().readTree(p);

		if (node.get("mphFunctionData") != null) try (JsonParser dataParser = node.traverse(p.getCodec())) {
			dataParser.nextToken();
			JavaType fullyResolvedDataType = ctxt.getTypeFactory().constructParametricType(MPHStringMap.SerializableData.class, valueType);
			MPHStringMap.SerializableData<?> data = ctxt.readValue(dataParser, fullyResolvedDataType);
			return MPHStringMap.fromData(data);
		}
		else {
			try (JsonParser fallbackParser = node.traverse(p.getCodec())) {
				fallbackParser.nextToken();
				return (Map<?, ?>) defaultDeserializer.deserialize(fallbackParser, ctxt);
			}
		}
	}
}
