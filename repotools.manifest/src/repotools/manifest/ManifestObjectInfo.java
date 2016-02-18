package repotools.manifest;

import java.io.File;

public class ManifestObjectInfo
{
	public ManifestObjectInfo(
		String name,
		ManifestDirectoryInfo parentDirectory)
	{
		this.name = name;
		this.parentDirectory = parentDirectory;
	}

	public String getName()
	{
		return name;
	}
	public ManifestDirectoryInfo getParentDirectory()
	{
		return parentDirectory;
	}
	public void setParentDirectory(ManifestDirectoryInfo parentDirectory)
	{
		this.parentDirectory = parentDirectory;
	}
	
	public File getFileObject()
	{
		if (parentDirectory != null)
		{
			return new File(parentDirectory.getFileObject(), name);
		}
		
		return new File(name);
	}

	private String name;
	private ManifestDirectoryInfo parentDirectory;
}
