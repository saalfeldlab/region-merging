package de.hanslovsky.regionmerging;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hanslovsky.graph.UndirectedGraph;
import de.hanslovsky.graph.edge.Edge;
import de.hanslovsky.graph.edge.EdgeMerger;
import de.hanslovsky.graph.edge.EdgeWeight;
import de.hanslovsky.util.unionfind.HashMapStoreUnionFind;
import gnu.trove.iterator.TLongIntIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;

public class RegionMerging
{

	public static Logger LOG = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	public static class Stringify
	{

		private final Supplier< String > generator;

		public Stringify( final Supplier< String > generator )
		{
			super();
			this.generator = generator;
		}

		@Override
		public String toString()
		{
			return generator.get();
		}

	}

	public static TLongArrayList mergeLocallyMinimalEdges( final UndirectedGraph g, final EdgeMerger merger, final EdgeWeight edgeWeight, final TLongLongHashMap counts, final double threshold )
	{
		return mergeLocallyMinimalEdges( g, merger, edgeWeight, counts, threshold, new TIntHashSet() );
	}

	public static TLongArrayList mergeLocallyMinimalEdges( final UndirectedGraph g, final EdgeMerger merger, final EdgeWeight edgeWeight, final TLongLongHashMap counts, final double threshold, final TIntHashSet nonContractingEdges )
	{
		final TDoubleArrayList edges = g.edges();
		final Edge e1 = new Edge( edges, merger.dataSize() );
		final Edge e2 = new Edge( edges, merger.dataSize() );

		final HashMapStoreUnionFind dj = new HashMapStoreUnionFind();

		final TLongArrayList merges = new TLongArrayList();

//		for ( int k = 0; k < e1.size(); ++k )
//		{
//			e1.setIndex( k );
//			e1.weight( edgeWeight.weight( e1, counts.get( e1.from() ), counts.get( e1.to() ) ) );
//		}

		boolean changed = true;
		int iteration = 1;
		while ( changed )
		{

			changed = false;

			for ( int k = 0; k < e1.size(); ++k )
			{
				e1.setIndex( k );
				if ( e1.isValid() && e1.isStale() )
				{
					e1.weight( edgeWeight.weight( e1, counts.get( e1.from() ), counts.get( e1.to() ) ) );
					e1.setActive();
				}
				else if ( e1.isObsolete() )
					e1.weight( Double.MAX_VALUE );
			}
			final boolean[] localMinimum = new boolean[ e1.size() ];
			Arrays.fill( localMinimum, true );
			final boolean[] isInPlateau = new boolean[ e1.size() ];
			final HashMapStoreUnionFind plateausUnionFind = new HashMapStoreUnionFind();

			findMinimaAndPlateaus( g, localMinimum, isInPlateau, e1, e2, plateausUnionFind, threshold );

			final boolean[] isMinimumPlateau = new boolean[ e1.size() ];
			Arrays.fill( isMinimumPlateau, true );
			final TLongHashSet plateauRoots = new TLongHashSet();

			for ( int k = 0; k < localMinimum.length; ++k )
			{
				e1.setIndex( k );
				if ( e1.isObsolete() )
					continue;
				final boolean isMinimum = localMinimum[ k ];
				final boolean isPlateau = isInPlateau[ k ];
				final boolean isContractible = !nonContractingEdges.contains( k );
				if ( isMinimum && !isPlateau )
				{
					if ( isContractible && mergeEdge( g, e1, k, dj, merger, merges ) )
						changed = true;
				}
				else if ( isPlateau )
				{
					final long root = plateausUnionFind.findRoot( k );
					// no &&= in java
					isMinimumPlateau[ ( int ) root ] &= isMinimum && isContractible;
					if ( !plateauRoots.contains( root ) )
						plateauRoots.add( root );
				}
			}

			for ( int k = 0; k < localMinimum.length; ++k )
				if ( isInPlateau[ k ] )
				{
					final long root = plateausUnionFind.findRoot( k );
					if ( isMinimumPlateau[ ( int ) root ] )
					{
						e1.setIndex( k );
						if ( e1.isValid() && mergeEdge( g, e1, k, dj, merger, merges ) )
							changed = true;
					}
				}

			LOG.debug(
					"Finished iteration {} with {} local minima and {} plateau edges.",
					iteration,
					new Stringify( () -> "" + IntStream.range( 0, localMinimum.length ).mapToObj( i -> localMinimum[ i ] ).filter( m -> m ).count() ),
					new Stringify( () -> "" + IntStream.range( 0, localMinimum.length ).mapToObj( i -> isInPlateau[ i ] ).filter( m -> m ).count() ) );

			++iteration;
		}

		return merges;

	}

	public static void findMinimaAndPlateaus(
			final UndirectedGraph g,
			final boolean[] localMinimum,
			final boolean[] isInPlateau,
			final Edge e1,
			final Edge e2,
			final HashMapStoreUnionFind plateausUnionFind,
			final double threshold )
	{

		final long[] connectedNodes = new long[ 2 ];

		for ( int k = 0; k < e1.size(); ++k )
		{
			e1.setIndex( k );
			if ( e1.isObsolete() )
			{
				localMinimum[ k ] = false;
				continue;
			}

			plateausUnionFind.findRoot( k );

			final double w = e1.weight();
			if ( w > threshold )
			{
				localMinimum[ k ] = false;
				continue;
			}
			connectedNodes[ 0 ] = e1.from();
			connectedNodes[ 1 ] = e1.to();

			boolean isPlateau = false;

			double minimumValue = Double.MAX_VALUE;

			for ( final long nodeId : connectedNodes )
			{
				final TLongIntHashMap neighboringEdges = g.nodeEdgeMap().get( nodeId );
				for ( final TLongIntIterator it = neighboringEdges.iterator(); it.hasNext(); )
				{
					it.advance();
					final int otherEdgeIndex = it.value();

					if ( otherEdgeIndex == k )
						continue;

					if ( nodeId == it.key() )
						continue;

					e2.setIndex( otherEdgeIndex );
					final double w2 = e2.weight();

					minimumValue = Math.min( w2, minimumValue );

					if ( w == w2 )
					{
						isPlateau = true;
						plateausUnionFind.join( plateausUnionFind.findRoot( k ), plateausUnionFind.findRoot( otherEdgeIndex ) );
					}

				}
			}

			localMinimum[ k ] = w <= minimumValue;
			isInPlateau[ k ] = isPlateau;
		}
	}

	public static boolean mergeEdge( final UndirectedGraph g, final Edge e, final long index, final HashMapStoreUnionFind dj, final EdgeMerger merger, final TLongArrayList merges )
	{
		final long r1 = dj.findRoot( e.from() );
		final long r2 = dj.findRoot( e.to() );
		if ( r1 == r2 )
			return false;

		final long newNode = dj.join( r1, r2 );
		g.contract( e, newNode, r1, r2, merger );
		merges.add( index );
		merges.add( Double.doubleToRawLongBits( e.weight() ) );
		return true;
	}



}
