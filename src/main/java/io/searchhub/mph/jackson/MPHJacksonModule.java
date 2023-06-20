package io.searchhub.mph.jackson;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.searchhub.mph.MPHStringMap;

public class MPHJacksonModule extends SimpleModule {

	public MPHJacksonModule() {
		super.addSerializer(MPHStringMap.class, new MPHStringMapSerializer());
		super.addDeserializer(MPHStringMap.class, new MPHStringMapDeserializer());

		super.setMixInAnnotation(Map.class, MapAnnotations.class);
	}

	@JsonTypeInfo(
			use = JsonTypeInfo.Id.CLASS,
			property = "type",
			defaultImpl = LinkedHashMap.class)
	public interface MapAnnotations {

	}
}
