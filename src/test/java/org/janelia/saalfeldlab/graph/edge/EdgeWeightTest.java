package org.janelia.saalfeldlab.graph.edge;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Random;

import org.janelia.saalfeldlab.graph.edge.EdgeWeight.MedianAffinityWeight;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EdgeWeightTest
{

	public static Logger LOG = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	@Test
	public void testMedianFromHistogram()
	{
		final int N = 1000000;
		final double min = -5.0;
		final double max = 5.0;
		final Random rng = new Random();
		final double[] samples = new double[ N ];
		for ( int i = 0; i < N; ++i )
			samples[ i ] = rng.nextDouble() * ( max - min ) + min;

		final int nBins = 100;
		final double binWidth = ( max - min ) / nBins;
		final long[] histogram = new long[ nBins ];
		for ( int i = 0; i < N; ++i )
		{
			final int index = Math.min( ( int ) ( ( samples[ i ] - min ) / binWidth ), nBins - 1 );
			++histogram[ index ];
		}
		Arrays.sort( samples );
		final double medianRef = samples[ N / 2 ];
		LOG.debug( medianRef + " " + MedianAffinityWeight.medianFromHistogram( nBins, Arrays.stream( histogram ), N, min, binWidth ) );
	}

}
