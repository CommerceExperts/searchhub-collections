package io.searchhub.mph;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

import io.searchhub.mph.MPHStringSet.SerializableData;
import org.junit.jupiter.api.Test;

class MPHStringSetTest {

	@Test
	public void standardUsageTest() {
		MPHStringSet mphSet = new MPHStringSet("a", "b", "c");
		assertTrue(mphSet.contains("a"));
		assertFalse(mphSet.contains("A"));
	}

	@Test
	public void borderCaseTest() {
		assertTrue(new MPHStringSet("").contains(""));
		assertFalse(new MPHStringSet(Collections.emptySet()).contains(""));
		assertFalse(new MPHStringSet(Collections.emptySet()).contains(null));
		assertFalse(new MPHStringSet("").contains(null));
	}

	@Test
	public void serializedWorksSimilarToInitial() {
		SerializableData dto = new MPHStringSet("a", "b", "c").toSerializable();
		MPHStringSet mphSet = new MPHStringSet(dto);
		assertTrue(mphSet.contains("a"));
		assertFalse(mphSet.contains("A"));
	}

}
