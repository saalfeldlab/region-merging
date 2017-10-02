package org.janelia.saalfeldlab.regionmerging;

import java.lang.invoke.MethodHandles;

import org.janelia.saalfeldlab.graph.UndirectedGraph;
import org.janelia.saalfeldlab.graph.edge.Edge;
import org.janelia.saalfeldlab.graph.edge.EdgeMerger;
import org.janelia.saalfeldlab.graph.edge.EdgeWeight;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongLongHashMap;

public class RegionMergingTest
{

	public static Logger LOG = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	@Test
	public void testSingleMerge()
	{

		LOG.debug( "Running test: single merge" );

		final TDoubleArrayList store = new TDoubleArrayList();
		final EdgeMerger merger = new EdgeMerger.MIN_AFFINITY_MERGER();
		final EdgeWeight ew = ( e, count1, count2 ) -> 1 - e.affinity();
		final Edge e = new Edge( store, merger.dataSize() );

		final double lowAffinity = 0.1;
		final double highAffinity = 0.9;

		e.add( Double.NaN, lowAffinity, 0, 2, 1 );
		e.add( Double.NaN, lowAffinity, 1, 2, 1 );
		e.add( Double.NaN, lowAffinity, 1, 5, 1 );
		e.add( Double.NaN, highAffinity, 2, 3, 1 );
		e.add( Double.NaN, lowAffinity, 2, 6, 1 );
		e.add( Double.NaN, lowAffinity, 3, 4, 1 );
		e.add( Double.NaN, lowAffinity, 3, 7, 1 );
		e.add( Double.NaN, lowAffinity, 4, 8, 1 );

		final TLongLongHashMap counts = new TLongLongHashMap();
		for ( int i = 0; i < e.size(); ++i )
		{
			e.setIndex( i );
			counts.put( e.from(), 1 );
			counts.put( e.to(), 1 );
			e.setStale();
			e.setValid();
		}

		final UndirectedGraph g = new UndirectedGraph( 9, store, merger );

		final TLongArrayList merges = RegionMerging.mergeLocallyMinimalEdges( g, merger, ew, counts, 0.5 ).getA();

		Assert.assertEquals( 4, merges.size() );
		Assert.assertEquals( 3, merges.get( 0 ) );

		e.setIndex( ( int ) merges.get( 0 ) );
		Assert.assertEquals( 1 - highAffinity, Double.longBitsToDouble( merges.get( 1 ) ), 1e-20 );
		Assert.assertFalse( e.isValid() );

		for ( int i = 0; i < e.size(); ++i )
		{
			e.setIndex( i );
			Assert.assertTrue( "Edge state for i=" + i + ": " + e.toString(), i == 3 ? e.isObsolete() : e.isValid() );
			if ( e.isValid() )
			{
				Assert.assertTrue( e.isActive() );
				Assert.assertEquals( lowAffinity, e.affinity(), 1e-20 );
				Assert.assertEquals( 1 - lowAffinity, e.weight(), 1e-20 );
			}
		}

	}

	@Test
	public void testConsecutiveMerge()
	{

		LOG.debug( "Running test: consecutive merge" );

		final TDoubleArrayList store = new TDoubleArrayList();
		final EdgeMerger merger = new EdgeMerger.MIN_AFFINITY_MERGER();
		final EdgeWeight ew = ( e, count1, count2 ) -> 1 - e.affinity();
		final Edge e = new Edge( store, merger.dataSize() );

		final double lowAffinity = 0.1;
		final double midAffinity = 0.5;
		final double highAffinity = 0.9;

		e.add( Double.NaN, lowAffinity, 0, 2, 1 );
		e.add( Double.NaN, lowAffinity, 1, 2, 1 );
		e.add( Double.NaN, lowAffinity, 1, 5, 1 );
		e.add( Double.NaN, highAffinity, 2, 3, 1 );
		e.add( Double.NaN, lowAffinity, 2, 6, 1 );
		e.add( Double.NaN, midAffinity, 3, 4, 1 );
		e.add( Double.NaN, lowAffinity, 3, 7, 1 );
		e.add( Double.NaN, lowAffinity, 4, 8, 1 );

		final TLongLongHashMap counts = new TLongLongHashMap();
		for ( int i = 0; i < e.size(); ++i )
		{
			e.setIndex( i );
			counts.put( e.from(), 1 );
			counts.put( e.to(), 1 );
			e.setValid();
			e.setStale();
		}

		final UndirectedGraph g = new UndirectedGraph( 9, store, merger );

		final TLongArrayList merges = RegionMerging.mergeLocallyMinimalEdges( g, merger, ew, counts, 0.8 ).getA();

		Assert.assertEquals( 8, merges.size() );
		Assert.assertEquals( 3, merges.get( 0 ) );
		Assert.assertEquals( 5, merges.get( 4 ) );
	}

	@Test
	public void testMergeAll()
	{

		LOG.debug( "Running test: merge everything" );

		final TDoubleArrayList store = new TDoubleArrayList();
		final EdgeMerger merger = new EdgeMerger.MIN_AFFINITY_MERGER();
		final EdgeWeight ew = ( e, count1, count2 ) -> 1 - e.affinity();
		final Edge e = new Edge( store, merger.dataSize() );

		final double lowAffinity = 0.1;
		final double midAffinity = 0.5;
		final double highAffinity = 0.9;

		e.add( Double.NaN, lowAffinity, 0, 2, 1 );
		e.add( Double.NaN, lowAffinity, 1, 2, 1 );
		e.add( Double.NaN, lowAffinity, 1, 5, 1 );
		e.add( Double.NaN, highAffinity, 2, 3, 1 );
		e.add( Double.NaN, lowAffinity, 2, 6, 1 );
		e.add( Double.NaN, midAffinity, 3, 4, 1 );
		e.add( Double.NaN, lowAffinity, 3, 7, 1 );
		e.add( Double.NaN, lowAffinity, 4, 8, 1 );

		final TLongLongHashMap counts = new TLongLongHashMap();
		for ( int i = 0; i < e.size(); ++i )
		{
			e.setIndex( i );
			counts.put( e.from(), 1 );
			counts.put( e.to(), 1 );
			e.setValid();
			e.setStale();
		}

		final UndirectedGraph g = new UndirectedGraph( 9, store, merger );

		final TLongArrayList merges = RegionMerging.mergeLocallyMinimalEdges( g, merger, ew, counts, 1.0 ).getA();

		Assert.assertEquals( 4 * e.size(), merges.size() );

		for ( int i = 0; i < e.size(); ++i )
		{
			e.setIndex( i );
			Assert.assertTrue( e.isObsolete() );
		}

	}

}
