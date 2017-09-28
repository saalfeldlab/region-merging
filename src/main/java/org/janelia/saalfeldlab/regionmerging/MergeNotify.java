package org.janelia.saalfeldlab.regionmerging;

public interface MergeNotify
{

	public void addMerge( long node1, long node2, long newNode, double weight );

}
