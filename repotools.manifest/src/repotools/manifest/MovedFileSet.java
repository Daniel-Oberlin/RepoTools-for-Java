package repotools.manifest;

import java.util.ArrayList;

public class MovedFileSet
{
	public MovedFileSet()
	{
		oldFiles = new ArrayList<ManifestFileInfo>();
		newFiles = new ArrayList<ManifestFileInfo>();
	}
	
	public ArrayList<ManifestFileInfo> getOldFiles() { return oldFiles; }
	public ArrayList<ManifestFileInfo> getNewFiles() { return newFiles; }
	
	// These aliases are used by RepoSync for clarity
	public ArrayList<ManifestFileInfo> getSourceFiles() { return oldFiles; }
	public ArrayList<ManifestFileInfo> getDestFiles() { return newFiles; }
	
	private ArrayList<ManifestFileInfo> oldFiles;
	private ArrayList<ManifestFileInfo> newFiles;
}
