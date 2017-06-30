package de.hanslovsky.graph;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import de.hanslovsky.graph.edge.Edge;
import de.hanslovsky.graph.edge.EdgeMerger;
import gnu.trove.iterator.TLongIntIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

public class UndirectedGraph implements Serializable
{

	public static final Logger LOG = LogManager.getLogger( MethodHandles.lookup().lookupClass() );
	{
		LOG.setLevel( Level.INFO );
	}

	private final TDoubleArrayList edges;

	private final TLongObjectHashMap< TLongIntHashMap > nodeEdgeMap;

	private final Edge e1, e2;

	public UndirectedGraph( final int nNodes, final EdgeMerger edgeMerger )
	{
		this( nNodes, new TDoubleArrayList(), edgeMerger );
	}

	public UndirectedGraph( final int nNodes, final TDoubleArrayList edges, final EdgeMerger edgeMerger )
	{
		this( edges, nodeEdgeMap( edges, nNodes, edgeMerger, edgeMerger.dataSize() ), edgeMerger.dataSize() );
	}

	public UndirectedGraph( final TDoubleArrayList edges, final TLongObjectHashMap< TLongIntHashMap > nodeEdgeMap, final int edgeDataSize )
	{
		this.edges = edges;
		this.nodeEdgeMap = nodeEdgeMap;
		this.e1 = new Edge( edges, edgeDataSize );
		this.e2 = new Edge( edges, edgeDataSize );
	}

	public TDoubleArrayList edges()
	{
		return edges;
	}

	public TLongObjectHashMap< TLongIntHashMap > nodeEdgeMap()
	{
		return nodeEdgeMap;
	}

	public int nNodes()
	{
		return nodeEdgeMap.size();
	}

	public TLongIntHashMap contract(
			final Edge e,
			final long newNode,
			final EdgeMerger edgeMerger )
	{
		assert newNode == e.from() || newNode == e.to(): "New node index must be either from or to index";

		final long from = e.from();
		final long to = e.to();

		final long otherNode = from == newNode ? to : from;

		e.setObsolete();

		final TLongIntHashMap edgesOfNewNode = nodeEdgeMap.get( newNode );
		final TLongIntHashMap discardEdges = nodeEdgeMap.get( otherNode );

		// remove e = (from, to) from node edge map (I)
		edgesOfNewNode.remove( otherNode );
		discardEdges.remove( newNode );

		// add all edges into edgesOfNewNode and merge/update if applicable
		for ( final TLongIntIterator discardIt = discardEdges.iterator(); discardIt.hasNext(); )
		{
			discardIt.advance();
			final long nodeId = discardIt.key();
			final int edgeId = discardIt.value();

			this.e1.setIndex( edgeId );

			if ( nodeId == otherNode || this.e1.isObsolete() )
			{
				this.e1.setObsolete();
				continue;
			}

			// merge edges if other node is connected to both nodes
			if ( edgesOfNewNode.contains( nodeId ) )
			{

				this.e2.setIndex( edgesOfNewNode.get( nodeId ) );
				final double w1 = this.e1.weight();
				final double w2 = this.e2.weight();

				// smaller weight edge is in wrong map
				if ( w1 < w2 )
				{
					edgeMerger.merge( this.e2, this.e1 );
					edgesOfNewNode.put( nodeId, edgeId );
					this.e2.setObsolete();
					this.e1.setStale();
					this.e1.setValid();
				}
				else
				{
					edgeMerger.merge( this.e1, this.e2 );
					this.e1.setObsolete();
					this.e2.setStale();
					this.e2.setValid();
				}
			}
			else
				edgesOfNewNode.put( nodeId, edgeId );
//				this.e1.setStale();
		}


		for ( final TLongIntIterator keepIt = edgesOfNewNode.iterator(); keepIt.hasNext(); )
		{
			keepIt.advance();
			final long nodeId = keepIt.key();
			final int edgeId = keepIt.value();

			final TLongIntHashMap otherMap = nodeEdgeMap.get( nodeId );
			otherMap.remove( from );
			otherMap.remove( to );
			otherMap.put( newNode, edgeId );
			this.e1.setIndex( edgeId );
			this.e1.setStale();
			this.e1.setValid();
			this.e1.from( nodeId );
			this.e1.to( newNode );
		}

		return discardEdges;

	}

//	public TLongIntHashMap contract(
//			final Edge e,
//			final long newNode,
//			final EdgeMerger edgeMerger )
//	{
//		assert newNode == e.from() || newNode == e.to(): "New node index must be either from or to index";
//
//		final long from = e.from();
//		final long to = e.to();
//
//		final long otherNode = from == newNode ? to : from;
//
//		e.setObsolete();
//
//		final TLongIntHashMap keepEdges = nodeEdgeMap.get( newNode );
//		final TLongIntHashMap discardEdges = nodeEdgeMap.get( otherNode );
//
//		keepEdges.remove( otherNode );
//		discardEdges.remove( newNode );
//
//		for ( final TLongIntIterator discardIt = discardEdges.iterator(); discardIt.hasNext(); )
//		{
//			discardIt.advance();
//			final long nodeId = discardIt.key();
//			final int edgeId = discardIt.value();
//
//			this.e1.setIndex( edgeId );
//
//			if ( nodeId == otherNode || this.e1.isObsolete() )
//			{
//				this.e1.setObsolete();
//				continue;
//			}
//
//			if ( keepEdges.contains( nodeId ) )
//			{
////				if ( keepEdges.get( nodeId ) == edgeId )
////				{
////					this.e1.setStale();
////					continue;
////				}
//
//				this.e2.setIndex( keepEdges.get( nodeId ) );
//				final double w1 = this.e1.weight();
//				final double w2 = this.e2.weight();
//
//				// smaller weight edge is in wrong map
//				if ( w1 < w2 )
//				{
//					edgeMerger.merge( this.e2, this.e1 );
//					keepEdges.put( nodeId, edgeId );
//					this.e2.setObsolete();
//					this.e1.setStale();
//					this.e1.setValid();
//				}
//				else
//				{
//					edgeMerger.merge( this.e1, this.e2 );
//					this.e1.setObsolete();
//					this.e2.setStale();
//					this.e2.setValid();
//				}
//			}
//			else
//				keepEdges.put( nodeId, edgeId );
////				this.e1.setStale();
//		}
//
//
//		for ( final TLongIntIterator keepIt = keepEdges.iterator(); keepIt.hasNext(); )
//		{
//			keepIt.advance();
//			final long nodeId = keepIt.key();
//			final int edgeId = keepIt.value();
//
//			final TLongIntHashMap otherMap = nodeEdgeMap.get( nodeId );
//			otherMap.remove( from );
//			otherMap.remove( to );
//			otherMap.put( newNode, edgeId );
//			this.e1.setIndex( edgeId );
//			this.e1.setStale();
//			this.e1.from( nodeId );
//			this.e1.to( newNode );
//		}
//
//		return discardEdges;
//
//	}

	private static TLongObjectHashMap< TLongIntHashMap > nodeEdgeMap( final TDoubleArrayList edges, final int nNodes, final EdgeMerger edgeMerger, final int edgeDataSize )
	{
		final TLongObjectHashMap< TLongIntHashMap > nodeEdgeMap = new TLongObjectHashMap<>();
		for ( int i = 0; i < nNodes; ++i )
			nodeEdgeMap.put( i, new TLongIntHashMap() );
		final Edge e1 = new Edge( edges, edgeDataSize );
		final Edge e2 = new Edge( edges, edgeDataSize );
		final int nEdges = e1.size();
		for ( int i = 0; i < nEdges; ++i )
		{
			e1.setIndex( i );
			if ( e1.isObsolete() )
				continue;
			final int from = ( int ) e1.from();
			final int to = ( int ) e1.to();

			assert from != to: e1;

			final TLongIntHashMap fromMap = nodeEdgeMap.get( from );
			final TLongIntHashMap toMap = nodeEdgeMap.get( to );

			if ( fromMap.contains( to ) || toMap.contains( from ) )
			{
				assert fromMap.get( to ) == toMap.get( from ): "Edges are inconsistent!";
				e2.setIndex( fromMap.get( to ) );
				LOG.trace( "Edge exists multiple times! " + e1 + " " + e2 + " " + fromMap + " " + toMap );
				edgeMerger.merge( e1, e2 );
				e2.setStale();
				e1.setObsolete();
			}
			else
			{
				fromMap.put( to, i );
				toMap.put( from, i );
			}

		}

		return nodeEdgeMap;
	}

}
