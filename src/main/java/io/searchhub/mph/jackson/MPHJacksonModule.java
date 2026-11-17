package io.searchhub.mph.jackson;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.MapType;
import io.searchhub.mph.MPHStringMap;
import io.searchhub.mph.PackageVersion;

public class MPHJacksonModule extends SimpleModule {

	public MPHJacksonModule() {
		super(PackageVersion.VERSION);
		super.addSerializer(MPHStringMap.class, new MPHStringMapSerializer());
		super.addDeserializer(MPHStringMap.class, new MPHStringMapDeserializer());

		super.setDeserializerModifier(new BeanDeserializerModifier() {

			@Override
			public JsonDeserializer<?> modifyMapDeserializer(DeserializationConfig config, MapType type, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
				return new DelegatingMapDeserializer(deserializer);
			}
		});
	}

}
