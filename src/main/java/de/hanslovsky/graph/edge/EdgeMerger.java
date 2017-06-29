package de.hanslovsky.graph.edge;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public interface EdgeMerger extends Serializable, EdgeDataSize
{
	public Edge merge( final Edge source, final Edge target );

	public static class MAX_AFFINITY_MERGER implements EdgeMerger, Serializable
	{

		@Override
		public Edge merge( final Edge source, final Edge target )
		{
			target.affinity( Math.max( source.affinity(), target.affinity() ) );
			target.multiplicity( source.multiplicity() + target.multiplicity() );
			return target;
		}

	}

	public static class MIN_AFFINITY_MERGER implements EdgeMerger, Serializable
	{

		@Override
		public Edge merge( final Edge source, final Edge target )
		{
			target.affinity( Math.min( source.affinity(), target.affinity() ) );
			target.multiplicity( source.multiplicity() + target.multiplicity() );
			return target;
		}

	}

	public static class AVG_AFFINITY_MERGER implements EdgeMerger, Serializable
	{

		@Override
		public Edge merge( final Edge source, final Edge target )
		{
			final long m1 = source.multiplicity();
			final long m2 = target.multiplicity();
			final long m = m1 + m2;
			target.affinity( ( m1 * source.affinity() + m2 * target.affinity() ) / m );
			target.multiplicity( m );
			return target;
		}

	}

	public static class MEDIAN_AFFINITY_MERGER implements EdgeMerger, Serializable
	{

		public static final Logger LOG = LogManager.getLogger( MethodHandles.lookup().lookupClass() );
		{
			LOG.setLevel( Level.INFO );
		}

		private final int nBins;

		private final int dataSize;

		public MEDIAN_AFFINITY_MERGER( final int nBins )
		{
			super();
			this.nBins = nBins;
			this.dataSize = nBins + 1;
		}

		@Override
		public Edge merge( final Edge source, final Edge target )
		{
			LOG.trace( "Merging edges: " + source + " " + target + " (sizes: " + source.size() + " " + target.size() + ")" );

			final long m1 = source.multiplicity();
			final long m2 = target.multiplicity();
			final long m = m1 + m2;
			target.multiplicity( m );

//			if ( target.from() == 12 && target.to() == 16 || target.from() == 16 && target.to() == 12 )
//			{
//				final List< String > dat1 = IntStream.range( 0, target.getDataSize() ).mapToObj( idx -> Edge.dtl( source.getData( idx ) ) + "" ).collect( Collectors.toList() );
//				final List< String > dat2 = IntStream.range( 0, target.getDataSize() ).mapToObj( idx -> Edge.dtl( source.getData( idx ) ) + "" ).collect( Collectors.toList() );
//				System.out.println( "MERGING EDGE INCLUDING 12 and 16 : " + target + " " + dat1 + " " + dat2 );
//			}

			for ( int entry = 0; entry < dataSize; ++entry )
			{
				final long d1 = Edge.dtl( source.getData( entry ) );
				final long d2 = Edge.dtl( target.getData( entry ) );
				if ( entry == 0 && ( d1 == 0 || d2 == 0 ) )
					// TODO remove this check
					throw new RuntimeException( "SOMETHING WRONG HERE!!!!" + source + " " + target + " " + dataSize + " " + source.getDataSize() + " " + target.getDataSize() );
				target.setData( entry, Edge.ltd( d1 + d2 ) );
			}

			return target;
		}

		@Override
		public int dataSize()
		{
			return dataSize;
		}

	}

}
