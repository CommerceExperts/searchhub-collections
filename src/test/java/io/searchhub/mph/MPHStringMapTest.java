package io.searchhub.mph;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MPHStringMapTest {

	Map<String, Integer> testData = new HashMap<>();
	MPHStringMap<Integer>         underTest;

	@BeforeEach
	public void setup() {
		Random random = new Random();
		for (int i = 1; i < 'Z'; i++) {
			String key = Character.toString(i) + " " + (i + random.nextInt());
			testData.put(key, i);
		}
		underTest = MPHStringMap.build(testData);
	}

	@Test
	void size() {
		assertEquals(testData.size(), underTest.size());
		assertEquals(testData.values().size(), underTest.values().size());
	}

	@Test
	void containsKey() {
		for (String key : testData.keySet()) {
			assertTrue(underTest.containsKey(key), "expected key '"+key+"' does not exist");
			String unexpectedKey = key + "xx";
			assertFalse(underTest.containsKey(unexpectedKey), "unexpected key "+unexpectedKey);
		}
	}

	@Test
	void containsValue() {
		Integer anyValue = testData.values().iterator().next();
		assertTrue(underTest.containsValue(anyValue), "value expected: "+anyValue);
	}

	@Test
	void get() {
		for (Map.Entry<String, Integer> testDataEntry : testData.entrySet()) {
			assertEquals(testDataEntry.getValue(), underTest.get(testDataEntry.getKey()));
		}
	}

	@Test
	void values() {
		Set<Integer> expectedValues = new HashSet<>(testData.values());
		for(Integer underTestValue : underTest.values()) {
			assertTrue(expectedValues.contains(underTestValue), "unexpected value: "+underTestValue);
		}
	}

	@Test
	void unsupportedMethods() {
		assertThrows(UnsupportedOperationException.class, () -> underTest.put("x", 1));
		assertThrows(UnsupportedOperationException.class, () -> underTest.putAll(Collections.singletonMap("x", 1)));
		assertThrows(UnsupportedOperationException.class, () -> underTest.forEach((k, v) -> {}));
		assertThrows(UnsupportedOperationException.class, () -> underTest.remove("a"));
		assertThrows(UnsupportedOperationException.class, underTest::clear);
		assertThrows(UnsupportedOperationException.class, underTest::entrySet);
		assertThrows(UnsupportedOperationException.class, underTest::keySet);
	}

	@Test
	void empty() {
		assertFalse(underTest.isEmpty());
		MPHStringMap<Object> emptyMap = MPHStringMap.build(Collections.emptyMap());
		assertTrue(emptyMap.isEmpty());
		assertEquals(0, emptyMap.size());
	}

	@Test
	void duplicateValues() {
		MPHStringMap<Boolean> hashSet = MPHStringMap.build(testData.keySet(), k -> Boolean.TRUE, 1);
		assertEquals(1, hashSet.values().size());

		Iterator<String> keyIterator = testData.keySet().iterator();
		assertSame(hashSet.get(keyIterator.next()), hashSet.get(keyIterator.next()));
	}
}
