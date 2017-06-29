package de.hanslovsky.regionmerging;

import java.util.Arrays;

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
		final int iteration = 1;
		while ( changed )
		{

			for ( int k = 0; k < e1.size(); ++k )
			{
				e1.setIndex( k );
				if ( e1.isValid() && e1.isStale() )
				{
					e1.weight( edgeWeight.weight( e1, counts.get( e1.from() ), counts.get( e1.to() ) ) );
					e1.setActive();
				}
			}

			changed = false;
			final boolean[] localMinimum = new boolean[ e1.size() ];
			Arrays.fill( localMinimum, true );
			final boolean[] isInPlateau = new boolean[ e1.size() ];

			final long[] connectedNodes = new long[ 2 ];

			final HashMapStoreUnionFind plateausUnionFind = new HashMapStoreUnionFind();

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

						e2.setIndex( otherEdgeIndex );
						final double w2 = e2.weight();

						if ( e2.isObsolete() )
							System.out.println( "Why is obsolete edge still in play?" + e2.toString() );

						minimumValue = Math.min( w2, minimumValue );

						if ( w == w2 && !isPlateau ) {
							isPlateau = true;
							plateausUnionFind.join( plateausUnionFind.findRoot( k ), plateausUnionFind.findRoot( otherEdgeIndex ) );
						}

					}
				}

				localMinimum[ k ] = minimumValue >= w;
				isInPlateau[ k ] = isPlateau;
			}

			final boolean[] isMinimumPlateau = new boolean[ e1.size() ];
			Arrays.fill( isMinimumPlateau, true );
			final TLongHashSet plateauRoots = new TLongHashSet();

			for ( int k = 0; k < localMinimum.length; ++k )
			{
				final boolean isMinimum = localMinimum[ k ];
//				System.out.println( k + " " + isMinimum + " " + isInPlateau[ k ] );
				if ( isMinimum && !isInPlateau[ k ] )
				{
					e1.setIndex( k );
					final long r1 = dj.findRoot( e1.from() );
					final long r2 = dj.findRoot( e1.to() );
//					System.out.println( "Contracting " + k + " " + r1 + " " + r2 );
					if ( r1 == r2 )
						continue;
					final long newNode = dj.join( r1, r2 );
					final TLongIntHashMap oldEdges = g.contract( e1, newNode, merger );
					e1.setObsolete();
//					for ( final TLongIntIterator it = oldEdges.iterator(); it.hasNext(); )
//					{
//						it.advance();
//						e1.setIndex( it.value() );
//						e1.setObsolete();
//					}
					merges.add( k );
					merges.add( Double.doubleToRawLongBits( e1.weight() ) );
					changed = true;
				}
				else if ( isInPlateau[ k ] )
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
						final long r1 = dj.findRoot( e1.from() );
						final long r2 = dj.findRoot( e1.to() );
						if ( r1 == r2 )
							continue;
						final long newNode = dj.join( r1, r2 );
						g.contract( e1, newNode, merger );
						merges.add( k );
						merges.add( Double.doubleToRawLongBits( e1.weight() ) );
						changed = true;
					}
				}

//			System.out.println( "At iteration " + iteration++ + " " + Arrays.toString( localMinimum ) + " " + Arrays.toString( isInPlateau ) );
		}

		return merges;

	}



}
