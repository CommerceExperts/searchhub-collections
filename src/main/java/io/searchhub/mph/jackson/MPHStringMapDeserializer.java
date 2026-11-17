package io.searchhub.mph.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.searchhub.mph.MPHStringMap;

import java.io.IOException;
import java.util.HashMap;

public class MPHStringMapDeserializer extends StdDeserializer<MPHStringMap<?>> implements ContextualDeserializer {

	private final JavaType mapValueType;

	protected MPHStringMapDeserializer() {
		this(TypeFactory.unknownType());
	}

	protected MPHStringMapDeserializer(JavaType valueType) {
		super(MPHStringMap.class);
		mapValueType = valueType;
	}

	@Override
	public MPHStringMap<?> deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);
		JsonNode dataNode = node.get("data");
		JavaType resolvedDataType = ctxt.getTypeFactory().constructParametricType(MPHStringMap.SerializableData.class, mapValueType);

		if (dataNode != null) try (JsonParser dataParser = dataNode.traverse()) {
			dataParser.setCodec(jsonParser.getCodec());
			dataParser.nextToken();
			MPHStringMap.SerializableData<?> deserializedData = ctxt.readValue(dataParser, resolvedDataType);
			return MPHStringMap.fromData(deserializedData);
		}
		else {
			HashMap<String, ?> hashMap = oc.treeToValue(node, HashMap.class);
			return MPHStringMap.build(hashMap);
		}
	}

	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
		JavaType type = (property != null) ? property.getType() : ctxt.getContextualType();

		if (type != null && type.isMapLikeType()) {
			JavaType valueType = type.getContentType(); // Extracts the 'V' from Map<K, V>
			return new MPHStringMapDeserializer(valueType);
		}
		else {
			return this;
		}
	}
}
