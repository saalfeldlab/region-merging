package de.hanslovsky.graph.edge;

import java.io.Serializable;
import java.util.PrimitiveIterator.OfDouble;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import gnu.trove.list.array.TDoubleArrayList;

public class Edge implements Serializable
{

	public static final int STALE_MASK = 1 << 0;

	public static final int OBSOLETE_MASK = 1 << 1;

	public static final int STATUS_MASK = STALE_MASK | OBSOLETE_MASK;

	public static final int COMMON_SIZE = 6;

	private final int dataSize;

	private final int stride;

	private final TDoubleArrayList data;

	private int k;

	public Edge( final TDoubleArrayList data, final int dataSize )
	{
		super();
		this.data = data;
		this.dataSize = dataSize;
		this.stride = COMMON_SIZE + dataSize;

		assert data.size() % stride == 0;
	}

	public int size()
	{
		return data.size() / stride;
	}

	public int getDataSize()
	{
		return dataSize;
	}

	public int getStride()
	{
		return stride;
	}

	public double getData( final int i )
	{
		return data.get( this.k + COMMON_SIZE + i );
	}

	public void setData( final int i, final double d )
	{
		data.set( this.k + COMMON_SIZE + i, d );
	}

	public void setIndex( final int k )
	{
		this.k = stride * k;
	}

	public double weight()
	{
		return data.get( k );
	}

	public void weight( final double weight )
	{
		data.set( k, weight );
	}

	public double affinity()
	{
		return data.get( k + 1 );
	}

	public void affinity( final double affinity )
	{
		data.set( k + 1, affinity );
	}

	public long from()
	{
		return dtl( data.get( k + 2 ) );
	}

	public void from( final long from )
	{
		data.set( k + 2, ltd( from ) );
	}

	public long to()
	{
		return dtl( data.get( k + 3 ) );
	}

	public void to( final long to )
	{
		data.set( k + 3, ltd( to ) );
	}

	public long multiplicity()
	{
		return dtl( data.get( k + 4 ) );
	}

	public void multiplicity( final long multiplicity )
	{
		data.set( k + 4, ltd( multiplicity ) );
	}

	public int status()
	{
		return ( int ) dtl( data.get( k + 5 ) );
	}

	public void status( final int status )
	{
		data.set( k + 5, ltd( status ) );
	}

	public boolean isStale()
	{
		return ( status() & STALE_MASK ) > 0;
	}

	public void setStale()
	{
		data.set( k + 5, ltd( status() | STALE_MASK ) );
	}

	public boolean isActive()
	{
		return !isStale();
	}

	public void setActive()
	{
		data.set( k + 5, ltd( status() & ~STALE_MASK ) );
	}

	public boolean isObsolete()
	{
		return ( status() & OBSOLETE_MASK ) > 0;
	}

	public void setObsolete()
	{
		data.set( k + 5, ltd( status() | OBSOLETE_MASK ) );
	}

	public boolean isValid()
	{
		return !isObsolete();
	}

	public void setValid() {
		data.set( k + 5, ltd( status() & ~OBSOLETE_MASK ) );
	}

	public void initialize( final double weight, final double affinity, final long from, final long to, final long multiplicity, final DoubleStream appendix )
	{
		weight( weight );
		affinity( affinity );
		from( from );
		to( to );
		multiplicity( multiplicity );
		setStale();
		setValid();
		final OfDouble it = appendix.iterator();
		for ( int i = 0; i < dataSize; ++i )
			setData( i, it.nextDouble() );
	}

	public int add( final double weight, final double affinity, final long from, final long to, final long multiplicity )
	{
		return add( weight, affinity, from, to, multiplicity, DoubleStream.generate( () -> 0.0d ) );
	}

	public int add( final double weight, final double affinity, final long from, final long to, final long multiplicity, final DoubleStream appendix )
	{
		final int index = size();
		data.add( weight );
		data.add( affinity );
		data.add( ltd( from ) );
		data.add( ltd( to ) );
		data.add( ltd( multiplicity ) );
		data.add( STALE_MASK );
		final OfDouble it = appendix.iterator();
		for ( int i = 0; i < dataSize; ++i )
			data.add( it.nextDouble() );
		return index;
	}

	public int add( final Edge e )
	{
		assert e.dataSize == dataSize;

		final int offset = e.k + COMMON_SIZE;

		return add( e.weight(), e.affinity(), e.from(), e.to(), e.multiplicity(), IntStream.range( offset, offset + dataSize ).mapToDouble( i -> e.data.get( i ) ) );
	}

	public int remove()
	{
		data.remove( k, stride );
		return size();
	}

	public TDoubleArrayList data()
	{
		return this.data;
	}

	public static double ltd( final long l )
	{
		return Double.longBitsToDouble( l );
	}

	public static long dtl( final double d )
	{
		return Double.doubleToLongBits( d );
	}

	@Override
	public String toString()
	{
		return "( " + k / stride + " , " + from() + " , " + to() + " , " + weight() + " , " + affinity() + " , " + multiplicity() + " , stale: " + isStale() + " , active: " + isActive() + " , obsolete: " + isObsolete() + " )";
	}

}
