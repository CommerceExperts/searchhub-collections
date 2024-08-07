package io.searchhub.mph;

import java.util.Set;
import java.util.function.Function;

import org.minperf.BitBuffer;
import org.minperf.RecSplitBuilder;
import org.minperf.RecSplitEvaluator;
import org.minperf.universal.StringHash;
import org.minperf.universal.UniversalHash;

class MPHUtil {

	public final static Function<String, Integer> EMPTY_MAP_FUNCTION = x -> -1;

	static byte[] getMphFunctionData(int leafSize, int avgBucketSize, Set<String> keys) {
		UniversalHash<String> hashFunction = new StringHash();
		BitBuffer mphFunctionData = RecSplitBuilder
				.newInstance(hashFunction)
				.leafSize(leafSize)
				.averageBucketSize(avgBucketSize)
				.generate(keys);
		return mphFunctionData.toByteArray();
	}

	static RecSplitEvaluator<String> buildEvaluator(int leafSize, int avgBucketSize, byte[] mphFunctionData) {
		UniversalHash<String> hashFunction = new StringHash();
		return RecSplitBuilder
				.newInstance(hashFunction)
				.leafSize(leafSize)
				.averageBucketSize(avgBucketSize)
				.buildEvaluator(new BitBuffer(mphFunctionData));
	}
}
