package io.searchhub.mph;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.minperf.RecSplitEvaluator;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static io.searchhub.mph.MPHUtil.buildEvaluator;
import static io.searchhub.mph.MPHUtil.getMphFunctionData;

/**
 * Immutable map using minimal perfect hashing for the keys + stores additional hash value per key to exclude non-existing keys.
 * <p>Since keys are not stored, it's not possible to use `keySet` and `entrySet`.</p>
 * <p>
 * Also since immutable, put, putAll, clear and remove will throw an UnsupportedOperationException.
 * </p>
 */
public class MPHStringIntMap implements Map<String, Integer> {

	private final static Function<String, Integer> EMPTY_MAP_FUNCTION = x -> -1;

	@RequiredArgsConstructor
	@AllArgsConstructor
	@Getter
	public final static class SerializableData implements Serializable {

		static SerializableData getEmptyData() {
			return new SerializableData(8, 32, new byte[0], new long[0]);
		}

		private static final long serialVersionUID = 1_000L;

		int    leafSize;
		int    avgBucketSize;
		byte[] mphFunctionData;
		long[] valueMap;

		public void setMphFunctionData(String base64Str) {
			this.mphFunctionData = Base64.getDecoder().decode(base64Str);
		}
	}

	public static MPHStringIntMap build(Map<String, Integer> inputData) {
		return build(inputData.keySet(), inputData::get);
	}

	/**
	 * Use this builder in case you have duplicate values that can be stored once.
	 * To use it, the exact amount of values has to be known.
	 * The values SHOULD implement equals and hashCode to allow a correct deduplication.
	 *
	 * @param keys        key-set
	 * @param valueLookup function to lookup a value for a key
	 * @return a map with all given keys and the values provided by the value lookup function
	 */
	public static MPHStringIntMap build(Set<String> keys, Function<String, Integer> valueLookup) {
		long[] valueEntries = new long[keys.size()];
		if (keys.isEmpty()) return new MPHStringIntMap(EMPTY_MAP_FUNCTION, SerializableData.getEmptyData());

		int leafSize = 8, avgBucketSize = 32;
		byte[] mphFunctionData = getMphFunctionData(leafSize, avgBucketSize, keys);
		SerializableData mphMapData = new SerializableData(leafSize, avgBucketSize, mphFunctionData, valueEntries);

		RecSplitEvaluator<String> recSplitEvaluator = buildEvaluator(leafSize, avgBucketSize, mphFunctionData);
		for (String key : keys) {
			int index = recSplitEvaluator.evaluate(key);
			int value = valueLookup.apply(key);
			valueEntries[index] = getVerifiableValue(key, value);
		}

		return new MPHStringIntMap(recSplitEvaluator::evaluate, mphMapData);
	}

	public static MPHStringIntMap build(Iterable<Entry<String, Integer>> keyValueIterable, int size) {
		AtomicReference<Entry<String, Integer>> currentEntry = new AtomicReference<>();
		Set<String> keySetEmulator = new AbstractSet<String>() {

			@Override
			public Iterator<String> iterator() {
				Iterator<Entry<String, Integer>> kvIterator = keyValueIterable.iterator();
				return new Iterator<String>() {

					@Override
					public boolean hasNext() {
						return kvIterator.hasNext();
					}

					@Override
					public String next() {
						Entry<String, Integer> next = kvIterator.next();
						currentEntry.set(next);
						return next.getKey();
					}
				};
			}

			@Override
			public int size() {
				return size;
			}
		};
		return build(keySetEmulator, key -> {
			Entry<String, Integer> entry = currentEntry.get();
			if (key != null && key.equals(entry.getKey())) {
				return entry.getValue();
			}
			else {
				return null;
			}
		});
	}

	public static MPHStringIntMap fromData(SerializableData data) {
		Function<String, Integer> mphFunction = (data.mphFunctionData.length == 0) ? x -> -1 : buildEvaluator(data.leafSize, data.avgBucketSize, data.mphFunctionData)::evaluate;
		return new MPHStringIntMap(mphFunction, data);
	}

	private MPHStringIntMap(Function<String, Integer> mphFunction, SerializableData data) {
		this.mphFunction = mphFunction;
		this.serializableMphMapData = data;
		this.valueMap = data.valueMap;
	}

	@Getter
	private final SerializableData          serializableMphMapData;
	private final Function<String, Integer> mphFunction;
	private final long[]                    valueMap;

	private static long getVerifiableValue(String originalKey, int valueIndex) {
		long encoded = originalKey.hashCode();
		encoded <<= 32;
		encoded |= valueIndex;
		return encoded;
	}

	private static Integer getVerifiedValue(long key, String searchKey) {
		int keyChecksum = (int) (key >>> 32);
		return keyChecksum == searchKey.hashCode() ? (int) key : null;
	}

	private Integer getValue(String searchKey) {
		int index = mphFunction.apply(searchKey);
		return getVerifiedValue(valueMap[index], searchKey);
	}

	@Override
	public int size() {
		return valueMap.length;
	}

	@Override
	public boolean isEmpty() {
		return valueMap.length == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return getValue(key.toString()) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		if (value == null) return false;
		if (!(value instanceof Integer)) return false;
		int seekValue = (int) value;
		boolean result = false;
		for (long valueEntry : valueMap) {
			if (seekValue == (int) valueEntry) {
				result = true;
				break;
			}
		}
		return result;
	}

	@Override
	public Integer get(Object key) {
		return getValue(key.toString());
	}

	@Override
	public Collection<Integer> values() {
		Integer[] values = new Integer[valueMap.length];
		for (int i = 0; i < values.length; i++) {
			// last 32 bits are the value itself
			values[i] = (int) valueMap[i];
		}
		return Arrays.asList(values);
	}

	/**
	 * @throws UnsupportedOperationException due to immutability
	 */
	@Override
	public Integer put(String key, Integer value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException due to immutability
	 */
	@Override
	public Integer remove(Object key) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException due to immutability
	 */
	@Override
	public void putAll(Map<? extends String, ? extends Integer> m) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException due to immutability
	 */
	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException since keys are not stored with the map
	 */
	@Override
	public Set<String> keySet() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException since keys are not stored with the map
	 */
	@Override
	public Set<Entry<String, Integer>> entrySet() {
		throw new UnsupportedOperationException();
	}
}
