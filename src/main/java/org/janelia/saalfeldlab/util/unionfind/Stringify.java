package org.janelia.saalfeldlab.util.unionfind;

import java.util.function.Supplier;

public class Stringify
{

	private final Supplier< String > toString;

	public Stringify( final Supplier< String > toString )
	{
		super();
		this.toString = toString;
	}

	@Override
	public String toString()
	{
		return toString.get();
	}

}
