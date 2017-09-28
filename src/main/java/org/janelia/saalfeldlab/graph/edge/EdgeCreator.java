package org.janelia.saalfeldlab.graph.edge;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface EdgeCreator extends EdgeDataSize
{

	public default int create( final Edge e, final double weight, final double affinity, final long from, final long to, final long multiplicity )
	{
		return e.add( weight, affinity, from, to, multiplicity, createData( weight, affinity, from, to, multiplicity ) );
	}

	public DoubleStream createData( final double weight, final double affinity, final long from, final long to, final long multiplicity );


//	public default Edge edge( final TDoubleArrayList data )
//	{
//		return new Edge( data, dataSize() );
//	}

	public static interface SerializableCreator extends EdgeCreator, Serializable
	{

	}

	public static class NoDataSerializableCreator implements SerializableCreator
	{

		@Override
		public DoubleStream createData( final double weight, final double affinity, final long from, final long to, final long multiplicity )
		{
			return DoubleStream.generate( () -> 0.0 ).limit( 0 );
		}

	}

	public static abstract class DataStreamCreator implements EdgeCreator
	{

		@Override
		public int create( final Edge e, final double weight, final double affinity, final long from, final long to, final long multiplicity )
		{
			return e.add( weight, affinity, from, to, multiplicity, createData( weight, affinity, from, to, multiplicity ) );
		}

	}

	public static class ComposedCreator extends DataStreamCreator
	{

		private final DataStreamCreator[] creators;

		private final int dataSize;

		public ComposedCreator( final DataStreamCreator... creators )
		{
			super();
			this.creators = creators;
			this.dataSize = Arrays.stream( creators ).mapToInt( c -> c.dataSize() ).sum();
		}

		@Override
		public DoubleStream createData( final double weight, final double affinity, final long from, final long to, final long multiplicity )
		{
			return Arrays.stream( creators ).map( c -> c.createData( weight, affinity, from, to, multiplicity ) ).reduce( ( s1, s2 ) -> DoubleStream.concat( s1, s2 ) ).get();
		}

		@Override
		public int dataSize()
		{
			return dataSize;
		}

	}

	public static class AffinityHistogram extends DataStreamCreator implements EdgeCreator, Serializable
	{

		public static final Logger LOG = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

		private final int nBins;

		private final double min;

		private final double max;

		private final double binWidth;

		public AffinityHistogram( final int nBins, final double min, final double max )
		{
			super();
			this.nBins = nBins;
			this.min = min;
			this.max = max;
			this.binWidth = ( max - min ) / nBins;
		}

//		@Override
//		public int create( final Edge e, final double weight, final double affinity, final long from, final long to, final long multiplicity )
//		{
//			// first entry: number of bins
//			assert e.getDataSize() == nBins + 1;
//
//			final int bin = ( int ) ( ( affinity - min ) / binWidth );
//			LOG.trace( "Creating edge: " + e.getDataSize() + " " + nBins + " " + bin );
//
//			final int index = e.add( weight, affinity, from, to, multiplicity, IntStream.range( 0, nBins + 1 ).mapToDouble( i -> Edge.ltd( i == 0 ? 1 : i == bin + 1 ? 1 : 0 ) ) );
//
//			e.setIndex( index );
//			LOG.trace( "Created edge with count: " + Edge.dtl( e.getData( 0 ) ) );
//
//			if ( Edge.dtl( e.getData( 0 ) ) == 0 )
//				throw new RuntimeException( "WAAAT??" );
//
//			return index;
//		}

		@Override
		public int dataSize()
		{
			return this.nBins + 1;
		}

		@Override
		public DoubleStream createData( final double weight, final double affinity, final long from, final long to, final long multiplicity )
		{
			final int bin = Math.min( ( int ) ( ( affinity - min ) / binWidth ), nBins - 1 );
			return IntStream.range( 0, dataSize() ).mapToDouble( i -> Edge.ltd( i == 0 ? 1 : i == bin + 1 ? 1 : 0 ) );
		}

	}

}
