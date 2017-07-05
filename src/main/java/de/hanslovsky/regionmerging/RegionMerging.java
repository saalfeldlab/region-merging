package de.hanslovsky.regionmerging;

import java.util.Arrays;
import java.util.stream.IntStream;

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
				if ( isMinimum && !isPlateau )
				{
					if ( mergeEdge( g, e1, k, dj, merger, merges ) )
						changed = true;
				}
				else if ( isPlateau )
				{
					final long root = plateausUnionFind.findRoot( k );
					isMinimumPlateau[ ( int ) root ] &= isMinimum;
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

//			System.out.println( "At iteration " + iteration + " " + Arrays.toString( localMinimum ) + " " + Arrays.toString( isInPlateau ) );
			System.out.println( "At iteration " + iteration + " " +
					IntStream.range( 0, localMinimum.length ).mapToObj( i -> localMinimum[ i ] ).filter( m -> m ).count() + " local minima " +
					IntStream.range( 0, localMinimum.length ).mapToObj( i -> isInPlateau[ i ] ).filter( m -> m ).count() + " plateau edges -- changed? " + changed );

			if ( !changed )
				for ( int i = 0; i < localMinimum.length; ++i )
					if ( !localMinimum[ i ] && isInPlateau[ i ] )
					{
						e1.setIndex( i );

						final long r1 = dj.findRoot( e1.from() );
						final long r2 = dj.findRoot( e1.to() );

						System.out.println( " WAAAT  ?" + i + " " + localMinimum[ i ] + " " + isInPlateau[ i ] + " " + e1.isValid() + " " + r1 + " " + r2 + " " + e1.toString() );
					}
//				e1.setIndex( 270 );
//				System.out.println( "270: " + localMinimum[ 270 ] + " " + e1.isValid() + " " + isInPlateau[ 270 ] + " " + e1 );
//				System.out.println( g.nodeEdgeMap().get( e1.from() ) );
//				System.out.println( g.nodeEdgeMap().get( e1.to() ) );

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

					if ( e2.isObsolete() )
					{
						System.out.println( "Why is obsolete edge still in play?" + e2.isValid() + " " + e2.isObsolete() + " " + e2.toString() );
						continue;
					}

					minimumValue = Math.min( w2, minimumValue );

					if ( k == 227 )
						System.out.println( "K=" + k + " other=" + otherEdgeIndex + " " + w + " " + w2 + " " + ( w2 < w ) + " " + e1.isValid() + " " + e2.isValid() );

					if ( w == w2 )
					{
						isPlateau = true;
						plateausUnionFind.join( plateausUnionFind.findRoot( k ), plateausUnionFind.findRoot( otherEdgeIndex ) );
					}

//					if ( isPlateau )
//						System.out.print( "WAS DA LOS EY? " + e1 + " " + e2 );

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
		g.contract( e, newNode, merger );
		merges.add( index );
		merges.add( Double.doubleToRawLongBits( e.weight() ) );
		return true;
	}



}
