package org.janelia.saalfeldlab.graph.edge;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.PrimitiveIterator.OfLong;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.janelia.saalfeldlab.util.unionfind.Stringify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface EdgeWeight extends EdgeDataSize
{

	public static final Logger LOG = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	public double weight( Edge e );

	public static class OneMinusAffinity implements EdgeWeight, Serializable
	{

		@Override
		public double weight( final Edge e )
		{
			return 1.0 - e.affinity();
		}

	}

	public static class MedianAffinityWeight implements EdgeWeight, Serializable
	{

		public static final Logger LOG = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

		private final int nBins;

		private final double min;

		private final double max;

		private final double binWidth;

		public MedianAffinityWeight( final int nBins, final double min, final double max )
		{
			super();
			this.nBins = nBins;
			this.min = min;
			this.max = max;
			this.binWidth = ( max - min ) / nBins;
		}

		@Override
		public double weight( final Edge e )
		{
			// as in
			// http://math.stackexchange.com/questions/879052/how-to-find-mean-and-median-from-histogram
			final long count = Edge.dtl( e.getData( 0 ) );
			final LongStream bins = IntStream.range( 1, nBins + 1 ).mapToLong( i -> Edge.dtl( e.getData( i ) ) );
			final double medianAffinity = medianFromHistogram( nBins, bins, count, min, binWidth );

			LOG.trace( "{}", new Stringify( () -> String.format( "Setting weight: %f %f %s %d", medianAffinity, 1 - medianAffinity, Arrays.toString( IntStream.range( 1, nBins + 1 ).mapToLong( i -> Edge.dtl( e.getData( i ) ) ).toArray() ), count ) ) );


			return 1 - medianAffinity;
		}

		@Override
		public int dataSize()
		{
			return nBins + 1;
		}

		public static double medianFromHistogram( final int nBins, final LongStream bins, final long count, final double min, final double binWidth )
		{
			long nVisitedBeforeMedian = 0;
			int lastBinBeforeMedian = -1;
			long countAt = -1;
			final OfLong it = bins.iterator();
			for ( int currentBin = 0; it.hasNext(); ++currentBin )
			{
				countAt = it.next().longValue();
				if ( 2 * ( nVisitedBeforeMedian + countAt ) > count )
					break;
				++lastBinBeforeMedian;
				nVisitedBeforeMedian += countAt;
			}

			// need to add 1 because bins indices are zero-based (need to now
			// number of bins, not bin index)
			final double lower = min + ( lastBinBeforeMedian + 1 ) * binWidth;
			final double median = lower + ( 0.5 * count - nVisitedBeforeMedian ) / countAt * binWidth;

			LOG.trace( "Calculating median: {} {} {} {} {}", lower, median, lastBinBeforeMedian, countAt, count );

			return median;
		}

	}

	public static class PercentileAffinityWeight implements EdgeWeight, Serializable
	{
		public static final Logger LOG = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

		private final int nBins;

		private final double min;

		private final double max;

		private final double binWidth;

		private final double percentile;

		public PercentileAffinityWeight( final int nBins, final double min, final double max, final double percentile )
		{
			super();
			this.nBins = nBins;
			this.min = min;
			this.max = max;
			this.binWidth = ( max - min ) / nBins;
			this.percentile = percentile;
		}

		@Override
		public double weight( final Edge e )
		{
			// as in
			// http://math.stackexchange.com/questions/879052/how-to-find-mean-and-median-from-histogram
			final long count = Edge.dtl( e.getData( 0 ) );
			final LongStream bins = IntStream.range( 1, nBins + 1 ).mapToLong( i -> Edge.dtl( e.getData( i ) ) );
			final double medianAffinity = percentileFromHistogram( nBins, bins, count, min, binWidth, percentile );

			LOG.trace( "{}", new Stringify( () -> String.format( "Setting weight: %f %f %s %d", medianAffinity, 1 - medianAffinity, Arrays.toString( IntStream.range( 1, nBins + 1 ).mapToLong( i -> Edge.dtl( e.getData( i ) ) ).toArray() ), count ) ) );

			return 1 - medianAffinity;
		}

		@Override
		public int dataSize()
		{
			return nBins + 1;
		}

		public static double percentileFromHistogram( final int nBins, final LongStream bins, final long count, final double min, final double binWidth, final double percentile )
		{
			long nVisitedBeforePercentile = 0;
			int lastBinBeforePercentile = -1;
			final double N = count * percentile;
			long countAt = -1;
			final OfLong it = bins.iterator();
			for ( int currentBin = 0; it.hasNext(); ++currentBin )
			{
				countAt = it.next().longValue();
				if ( nVisitedBeforePercentile + countAt >= N )
					break;
				++lastBinBeforePercentile;
				nVisitedBeforePercentile += countAt;
			}

			// need to add 1 because bins indices are zero-based (need to now
			// number of bins, not bin index)
			final double lower = min + ( lastBinBeforePercentile + 1 ) * binWidth;
			final double median = lower + ( N - nVisitedBeforePercentile ) / countAt * binWidth;

			LOG.trace( "Calculating median: {} {} {} {} {}", lower, median, lastBinBeforePercentile, countAt, count );

			return median;
		}
	}

}
