package de.hanslovsky.util.unionfind;

import gnu.trove.map.hash.TLongLongHashMap;

public class HashMapStoreUnionFind
{
	private final TLongLongHashMap parents;

	private final TLongLongHashMap ranks;

	private int nSets;

	public HashMapStoreUnionFind()
	{
		this( 0 );
	}

	public HashMapStoreUnionFind( final int size )
	{
		this.parents = new TLongLongHashMap();
		this.ranks = new TLongLongHashMap();
		this.nSets = size;
		for ( int i = 0; i < size; ++i ) {
			this.parents.put( i, i );
			this.ranks.put( i, 0 );
		}
	}

	public HashMapStoreUnionFind( final TLongLongHashMap parents, final TLongLongHashMap ranks, final int nSets )
	{
		this.parents = parents;
		this.ranks = ranks;
		this.nSets = nSets;
	}

	public long findRoot( final long id )
	{

		if ( !this.parents.contains( id ) )
		{
			this.parents.put( id, id );
			this.ranks.put( id, 0 );
			++nSets;
			return id;
		}

		long startIndex1 = id;
		long startIndex2 = id;
		long tmp = id;


		// find root
		while ( startIndex1 != parents.get( startIndex1 ) )
			startIndex1 = parents.get( startIndex1 );

		// label all positions on the way to root as parent
		while ( startIndex2 != startIndex1 )
		{
			tmp = parents.get( startIndex2 );
			parents.put( startIndex2, startIndex1 );
			startIndex2 = tmp;
		}

		return startIndex1;

	}

	public long join( final long id1, final long id2 )
	{

		if ( !parents.contains( id1 ) )
		{
			parents.put( id1, id1 );
			ranks.put( id1, 0 );
			++nSets;
		}

		if ( !parents.contains( id2 ) )
		{
			parents.put( id2, id2 );
			ranks.put( id2, 0 );
			++nSets;
		}

		if ( id1 == id2 )
			//			assert this.parents.contains( id1 ) && this.parents.contains( id2 );
			return id1;

		--nSets;

		final long r1 = ranks.get( id1 );
		final long r2 = ranks.get( id2 );

		if ( r1 < r2 )
		{
			parents.put( id1, id2 );
			return id2;
		}

		else
		{
			parents.put( id2, id1 );
			if ( r1 == r2 )
				ranks.put( id1, r1 + 1 );
			return id1;
		}

	}

	public int size()
	{
		return parents.size();
	}

	public int setCount()
	{
		return nSets;
	}

	@Override
	public HashMapStoreUnionFind clone()
	{
		final TLongLongHashMap parents = new TLongLongHashMap();
		parents.putAll( this.parents );
		final TLongLongHashMap ranks = new TLongLongHashMap();
		ranks.putAll( this.ranks );
		return new HashMapStoreUnionFind( parents, ranks, nSets );
	}

}
