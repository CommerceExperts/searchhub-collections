package io.searchhub.mph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JsonSerializationTest {

	Map<String, String> testData = new HashMap<>();
	private MPHStringMap<String> underTest;

	@BeforeEach
	public void setup() {
		Random random = new Random();
		for (int i = 1; i < 'Z'; i++) {
			String key = Character.toString(i) + " " + (i + random.nextInt());
			String value = "Value:"+(i + random.nextInt())+Character.toString(i);
			testData.put(key, value);
		}
		underTest = MPHStringMap.build(testData);
	}

	@Test
	public void testMPHStringMap() throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		String serializedMPHData = mapper.writeValueAsString(underTest.getSerializableMphMapData());

		MPHStringMap.SerializableData<String> deserializedData = mapper.readValue(serializedMPHData, new TypeReference<>() {});
		MPHStringMap<String> deserializedMPH = MPHStringMap.fromData(deserializedData);

		for(Map.Entry<String, String> entry : testData.entrySet()) {
			assertEquals(underTest.get(entry.getKey()), deserializedMPH.get(entry.getKey()));

			assertNull(underTest.get(entry.getKey()+"troll"));
			assertNull(deserializedMPH.get(entry.getKey()+"troll"));
		}
	}
}
