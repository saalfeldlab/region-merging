package org.janelia.saalfeldlab.regionmerging;

import java.lang.invoke.MethodHandles;
import java.util.Random;

import org.janelia.saalfeldlab.graph.UndirectedGraph;
import org.janelia.saalfeldlab.graph.edge.Edge;
import org.janelia.saalfeldlab.graph.edge.EdgeCreator;
import org.janelia.saalfeldlab.graph.edge.EdgeMerger;
import org.janelia.saalfeldlab.graph.edge.EdgeWeight;
import org.janelia.saalfeldlab.graph.edge.EdgeWeight.MedianAffinityWeight;
import org.janelia.saalfeldlab.regionmerging.RegionMerging;
import org.janelia.saalfeldlab.util.unionfind.HashMapStoreUnionFind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class RegionMergingExample
{

	public static Logger LOG = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	public static void main( final String[] args )
	{

		final String HOME_DIR = System.getProperty( "user.home" );
//		final String affPath = HOME_DIR + "/Dropbox/misc/excerpt2D-12x12-aff.tif";
//		final String zwsPath = HOME_DIR + "/Dropbox/misc/excerpt2D-12x12-zws.tif";
		final String affPath = HOME_DIR + "/Dropbox/misc/excerpt2D-aff.tif";
		final String zwsPath = HOME_DIR + "/Dropbox/misc/excerpt2D-zws-16bit.tif";

		final ImagePlus affp = new ImagePlus( affPath );
		final ImagePlus zwsp = new ImagePlus( zwsPath );

		final RandomAccessibleInterval< FloatType > aff = ImageJFunctions.wrapFloat( affp );
		final RandomAccessibleInterval< UnsignedShortType > zws = ArrayImgs.unsignedShorts( ( short[] ) zwsp.getProcessor().getPixels(), zwsp.getWidth(), zwsp.getHeight() );

		final int nBins = 256;
		final EdgeMerger merger = new EdgeMerger.MEDIAN_AFFINITY_MERGER( nBins );
		final EdgeCreator creator = new EdgeCreator.AffinityHistogram( nBins, 0.0, 1.0 );

		final TLongObjectHashMap< TLongIntHashMap > nodeEdgeMap = new TLongObjectHashMap<>();

		final TDoubleArrayList edgeStore = new TDoubleArrayList();
		final Edge e = new Edge( edgeStore, creator.dataSize() );
		final Edge dummy = new Edge( new TDoubleArrayList(), creator.dataSize() );
		dummy.setIndex( 0 );

		final int nDim = zws.numDimensions();
		for ( int d = 0; d < nDim; ++d )
		{
//			final IntervalView< FloatType > affHs = Views.hyperSlice( aff, nDim, nDim - 1 - d );
			final IntervalView< FloatType > affHs = Views.hyperSlice( aff, nDim, d );

			final long[] min1 = Intervals.minAsLongArray( zws );
			final long[] max1 = Intervals.maxAsLongArray( zws );
			max1[ d ] -= 1;
			final Cursor< UnsignedShortType > c1 = Views.flatIterable( Views.interval( zws, min1, max1 ) ).cursor();
			final RandomAccess< UnsignedShortType > access = zws.randomAccess();
			final Cursor< FloatType > a = Views.flatIterable( Views.interval( affHs, min1, max1 ) ).cursor();
			while ( a.hasNext() )
			{
				final float val = a.next().get();
				c1.fwd();

				if ( Float.isNaN( val ) )
					continue;

				final int l1 = c1.get().get();
				access.setPosition( c1 );
				access.fwd( d );
				final int l2 = access.get().get();

				if ( l1 == l2 )
					continue;

				if ( !nodeEdgeMap.contains( l1 ) )
					nodeEdgeMap.put( l1, new TLongIntHashMap() );

				if ( !nodeEdgeMap.contains( l2 ) )
					nodeEdgeMap.put( l2, new TLongIntHashMap() );

				final TLongIntHashMap m1 = nodeEdgeMap.get( l1 );
				final TLongIntHashMap m2 = nodeEdgeMap.get( l2 );

				final int lower = Math.min( l1, l2 );
				final int upper = Math.max( l1, l2 );

				if ( m1.contains( l2 ) )
				{
					final int idx = m1.get( l2 );
					assert m2.contains( l1 ) && m2.get( l1 ) == idx;

					e.setIndex( idx );

					creator.create( dummy, Double.NaN, val, lower, upper, 1 );
					merger.merge( dummy, e );
					dummy.remove();
				}
				else
				{
					m1.put( l2, e.size() );
					m2.put( l1, e.size() );
					creator.create( e, Double.NaN, val, lower, upper, 1 );
				}

			}

//			ImageJFunctions.show( Views.interval( zws, min, max ), "zws" );
//			ImageJFunctions.show( Views.interval( translated, min, max ), "translated" );
		}

		final UndirectedGraph g = new UndirectedGraph( edgeStore, nodeEdgeMap, creator.dataSize() );

		for ( int i = 0; i < e.size(); ++i )
		{
			e.setIndex( i );
			e.setStale();
			e.setValid();
		}

		final TLongLongHashMap counts = new TLongLongHashMap();
		final HashMapStoreUnionFind lut = new HashMapStoreUnionFind();
		final TLongIntHashMap cmap = new TLongIntHashMap();
		final Random rng = new Random( 110 );

		for ( final UnsignedShortType label : Views.flatIterable( zws ) )
		{
			final int lbl = label.get();
			if ( counts.contains( lbl ) )
				counts.put( lbl, counts.get( lbl ) + 1 );
			else
			{
				counts.put( lbl, 1 );
				lut.findRoot( lbl );
				cmap.put( lbl, rng.nextInt() );
			}

		}

		final MedianAffinityWeight weight = new EdgeWeight.MedianAffinityWeight( nBins, 0.0, 1.0 );

		final TLongArrayList merges = RegionMerging.mergeLocallyMinimalEdges( g, merger, weight, counts, 0.8, ( n1, n2, nn, w ) -> {} ).getA();

		for ( int i = 0; i < merges.size(); i += 2 )
		{
			e.setIndex( ( int ) merges.get( i ) );
			final long r1 = lut.findRoot( e.from() );
			final long r2 = lut.findRoot( e.to() );
			lut.join( r1, r2 );
		}

		final Converter< UnsignedShortType, ARGBType > cmapConverter = ( s, t ) -> {
			t.set( cmap.get( s.getIntegerLong() ) );
		};

		final RandomAccessibleInterval< ARGBType > converted = Converters.convert( zws, cmapConverter, new ARGBType() );

		final Converter< UnsignedShortType, UnsignedShortType > mergedConverter =
				( input, output ) -> output.set( ( int ) lut.findRoot( input.getIntegerLong() ) );

				final RandomAccessibleInterval< UnsignedShortType > merged = Converters.convert( zws, mergedConverter, new UnsignedShortType() );

				final RandomAccessibleInterval< ARGBType > mergedColor = Converters.convert( merged, cmapConverter, new ARGBType() );

				new ImageJ();
				ImageJFunctions.show( zws, "ock" );
				ImageJFunctions.show( converted, "orig" );
				ImageJFunctions.show( mergedColor, "merged" );
				LOG.info( "number of merges: " + merges.size() / 2 );
				ImageJFunctions.show( merged, "nocolor" );
				ImageJFunctions.show( aff, "aff" );

	}
}
