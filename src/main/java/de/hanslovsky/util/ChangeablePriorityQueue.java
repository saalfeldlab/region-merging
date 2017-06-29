package de.hanslovsky.util;

import java.util.Arrays;
import java.util.Random;

public class ChangeablePriorityQueue
{

	public static interface Compare
	{

		boolean isLess( double i1, double i2 );

	}

	public static Compare DEFAULT_COMPARE = ( d1, d2 ) -> d1 < d2;

	private final int maxSize;

	private int currentSize;

	private final int[] heap;

	private final int[] indices;

	private final double[] priorities;

	private final Compare comp;

	public ChangeablePriorityQueue( final int maxSize )
	{
		this( maxSize, DEFAULT_COMPARE );
	}

	public ChangeablePriorityQueue( final int maxSize, final Compare comp )
	{
		this.maxSize = maxSize;
		this.currentSize = 0;
		this.heap = new int[ maxSize + 1 ];
		this.indices = new int[ maxSize + 1 ];
		this.priorities = new double[ maxSize + 1 ];
		this.comp = comp;
		reset();
	}

	@Override
	public ChangeablePriorityQueue clone()
	{
		return new ChangeablePriorityQueue( this.maxSize, this.currentSize, this.heap.clone(), this.indices.clone(), this.priorities.clone(), this.comp );
	}

	private ChangeablePriorityQueue( final int maxSize, final int currentSize, final int[] heap, final int[] indices, final double[] priorities, final Compare comp )
	{
		this.maxSize = maxSize;
		this.currentSize = currentSize;
		this.heap = heap;
		this.indices = indices;
		this.priorities = priorities;
		this.comp = comp;
	}

	private void reset()
	{
		this.currentSize = 0;
		Arrays.fill( this.indices, -1 );
	}

	public boolean empty()
	{
		return this.size() == 0;
	}

	public void clear()
	{
		for ( int i = 0; i < this.currentSize; ++i )
		{
			this.indices[ this.heap[ i + 1 ] ] = -1;
			this.heap[ i + 1 ] = -1;
		}
		currentSize = 0;
	}

	public boolean contains( final int i )
	{
		return this.indices[ i ] != -1;
	}

	public int size()
	{
		return currentSize;
	}

	public void push( final int i, final double p )
	{
		if ( !contains( i ) )
		{
			++this.currentSize;
			this.indices[ i ] = this.currentSize;
			this.heap[ currentSize ] = i;
			this.priorities[ i ] = p;
			bubbleUp( currentSize );
		}
		else
			changePriority( i, p );
	}

	public int top()
	{
		return this.heap[ 1 ];
	}

	public double topPriority()
	{
		return this.priorities[ top() ];
	}

	public int pop()
	{
		final int min = top();
		swapItems( 1, this.currentSize-- );
		bubbleDown( 1 );
		indices[ min ] = -1;
		heap[ currentSize + 1 ] = -1;
		return min;
	}

	public double priority( final int i )
	{
		return priorities[ i ];
	}

	public void deleteItem( final int i )
	{
		final int ind = indices[ i ];
		swapItems( ind, currentSize-- );
		bubbleUp( ind );
		bubbleDown( ind );
		indices[ i ] = -1;
	}

	public void changePriority( final int i, final double p )
	{
		final double pOld = this.priority( i );
		priorities[ i ] = p;
		if ( gt( p, pOld ) )
			bubbleDown(indices[i]);
		else if ( lt( p, pOld ) )
			bubbleUp(indices[i]);
	}

	private void swapItems( final int i, final int k )
	{
		final int tmp = heap[i];
		heap[i] = heap[k];
		heap[k] = tmp;
		indices[ heap[i] ] = i;
		indices[ heap[k] ] = k;
	}

	private void bubbleUp( int k )
	{
		while ( k > 1 && gt( this.priorities[ heap[ k / 2 ] ], priorities[ heap[ k ] ] ) )
		{
			swapItems( k, k / 2 );
			k = k / 2;
		}
	}

	private void bubbleDown( int k )
	{
		int j;
		while ( 2 * k <= currentSize )
		{
			j = 2 * k;
			if ( j < currentSize && gt( priority( heap[ j ] ), priority( heap[ j + 1 ] ) ) )
				++j;
			if ( leqt( priority( heap[ k ] ), priority( heap[ j ] ) ) )
				break;
			swapItems( k, j );
			k = j;

		}
	}

	private boolean lt( final double a, final double b )
	{
		return comp.isLess( a, b );
	}

	private boolean leqt( final double a, final double b )
	{
		return !comp.isLess( b, a );
	}

	private boolean eq( final double a, final double b )
	{
		return !comp.isLess( a, b ) && !comp.isLess( b, a );
	}

	private boolean gt( final double a, final double b )
	{
		return !eq( a, b ) && !comp.isLess( a, b );
	}

	private boolean geqt( final double a, final double b )
	{
		return !comp.isLess( a, b );
	}

	public static void main( final String[] args )
	{
		final int nIter = 10;
		final ChangeablePriorityQueue q = new ChangeablePriorityQueue( nIter );
		final Random rng = new Random( 100 );
		for ( int i = 0; i < nIter; ++i )
		{
			final double v = 100 * rng.nextDouble();
			System.out.println( "Random val: " + v );
			q.push( i, v );
		}

		q.changePriority( 0, 100 );
		q.changePriority( 4, 3 );

		final ChangeablePriorityQueue q2 = q.clone();

		while ( !q.empty() )
			System.out.println( q.top() + " " + q.topPriority() + " " + q.pop() );

		q2.deleteItem( nIter / 2 );
		System.out.println();

		while ( !q2.empty() )
			System.out.println( q2.top() + " " + q2.topPriority() + " " + q2.pop() );

	}


}
