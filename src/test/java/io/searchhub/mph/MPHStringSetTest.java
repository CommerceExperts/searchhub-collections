package io.searchhub.mph;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.stream.IntStream;

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
	public void minimalCollisionTest() {
		MPHStringSet mphSet = new MPHStringSet("a");
		assertTrue(mphSet.contains("a"));
		IntStream.range('b', Character.MAX_VALUE).forEach(c -> assertFalse(mphSet.contains(String.valueOf((char) c))));
	}

	@Test
	public void borderCaseTest() {
		assertTrue(new MPHStringSet("").contains(""));
		assertFalse(new MPHStringSet(Collections.emptySet()).contains(""));
		assertFalse(new MPHStringSet(Collections.emptySet()).contains(null));
		assertFalse(new MPHStringSet("").contains(null));
	}

	@Test
	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	public void serializedWorksSimilarToInitial() {
		SerializableData dto = new MPHStringSet("Aa", "Ba", "Ca", "Da").toSerializable();
		MPHStringSet mphSet = new MPHStringSet(dto);
		assertTrue(mphSet.contains("Aa"));
		for (String hashCollisionStr : new String[] { "CB", "DB", "EB", "BB" }) {
			assertFalse(mphSet.contains(hashCollisionStr), hashCollisionStr + " is not in the set");
		}
	}

}
