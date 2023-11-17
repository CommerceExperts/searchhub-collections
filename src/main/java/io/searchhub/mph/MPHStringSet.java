package io.searchhub.mph;

import static io.searchhub.mph.MPHUtil.buildEvaluator;
import static io.searchhub.mph.MPHUtil.getMphFunctionData;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.minperf.RecSplitEvaluator;

/**
 * Unmodifiable set with constant size usage, that can only provide the 'contains' and 'size' methods, since the keys are not stored.
 * It can be used similar to bloom filter: build it in offsite batch process and transported to a low-memory application for filtering.
 */
public class MPHStringSet implements Set<String> {

	// saved for serialization
	private       int    leafSize      = 8;
	private       int    avgBucketSize = 32;
	private final byte[] mphFunctionData;

	private final Function<String, Integer> primaryHashFunction;
	private final int[]                     secondaryHashes;

	@RequiredArgsConstructor
	@AllArgsConstructor
	@Getter
	public final static class SerializableData implements Serializable {

		static MPHStringSet.SerializableData getEmptyData() {
			return new MPHStringSet.SerializableData(8, 32, new byte[0], new int[0]);
		}

		static final long serialVersionUID = 1_000L;

		int    leafSize;
		int    avgBucketSize;
		byte[] mphFunctionData;
		int[]  secondaryHashes;

		public void setMphFunctionData(String base64Str) {
			this.mphFunctionData = Base64.getDecoder().decode(base64Str);
		}
	}

	public MPHStringSet(String... keys) {
		this(toSet(keys));
	}

	private static HashSet<String> toSet(String[] keys) {
		HashSet<String> set = new HashSet<>(keys.length);
		Collections.addAll(set, keys);
		return set;
	}

	public MPHStringSet(Set<String> keys) {
		if (keys.isEmpty()) {
			secondaryHashes = new int[0];
			primaryHashFunction = k -> -1;
			mphFunctionData = null;
		} else {
			secondaryHashes = new int[keys.size()];
			mphFunctionData = getMphFunctionData(leafSize, avgBucketSize, keys);
			RecSplitEvaluator<String> recSplitEvaluator = buildEvaluator(leafSize, avgBucketSize, mphFunctionData);
			primaryHashFunction = recSplitEvaluator::evaluate;
			for (String key : keys) {
				int keyIndex = recSplitEvaluator.evaluate(key);
				secondaryHashes[keyIndex] = key.hashCode();
			}
		}
	}

	public MPHStringSet(SerializableData dto) {
		secondaryHashes = dto.secondaryHashes;
		leafSize = dto.leafSize;
		avgBucketSize = dto.avgBucketSize;
		mphFunctionData = dto.mphFunctionData;
		RecSplitEvaluator<String> recSplitEvaluator = buildEvaluator(dto.leafSize, dto.avgBucketSize, dto.mphFunctionData);
		primaryHashFunction = recSplitEvaluator::evaluate;
	}

	public SerializableData toSerializable() {
		return new SerializableData(leafSize, avgBucketSize, mphFunctionData, secondaryHashes);
	}

	@Override
	public int size() {
		return secondaryHashes.length;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	private boolean containsStr(String key) {
		if (isEmpty()) return false;
		int keyIndex = primaryHashFunction.apply(key);
		return secondaryHashes[keyIndex] == key.hashCode();
	}

	@Override
	public boolean contains(Object o) {
		return o instanceof String && containsStr((String) o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return c.stream().allMatch(this::contains);
	}

	/**
	 * @throws UnsupportedOperationException
	 * 		due to immutability
	 */
	@Override
	public Iterator<String> iterator() {
		throw new UnsupportedOperationException("cannot access keys");
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException("cannot access keys");
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException("cannot access keys");
	}

	@Override
	public boolean add(String s) {
		throw new UnsupportedOperationException("set cannot be modified");
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException("set cannot be modified");
	}

	@Override
	public boolean addAll(Collection<? extends String> c) {
		throw new UnsupportedOperationException("set cannot be modified");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("set cannot be modified");
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException("set cannot be modified");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("set cannot be modified");
	}
}
