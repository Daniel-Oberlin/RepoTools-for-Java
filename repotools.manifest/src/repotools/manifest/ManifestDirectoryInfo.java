package repotools.manifest;

import java.util.HashMap;
import java.util.Map;

public class ManifestDirectoryInfo extends ManifestObjectInfo
{
	public ManifestDirectoryInfo(
		String name,
		ManifestDirectoryInfo parentDirectory)
	{
		super(name, parentDirectory);
		this.files = new HashMap<String , ManifestFileInfo>();
		this.subdirectories = new HashMap<String , ManifestDirectoryInfo>();
	}
	
	public ManifestDirectoryInfo(
		ManifestDirectoryInfo original,
		ManifestDirectoryInfo parentDirectory)
	{
		super(original.getName(), parentDirectory);
		
		this.files = new HashMap<String , ManifestFileInfo>();
		this.subdirectories = new HashMap<String , ManifestDirectoryInfo>();

		for (String nextDirName : original.subdirectories.keySet())
		{
			ManifestDirectoryInfo dirClone =
				new ManifestDirectoryInfo(
					original.subdirectories.get(nextDirName),
					this);
		
			this.subdirectories.put(nextDirName, dirClone);
		}

		for (String nextFileName : original.files.keySet())
		{
			ManifestFileInfo fileClone =
				new ManifestFileInfo(
					original.files.get(nextFileName),
					this);
				
			files.put(nextFileName, fileClone);
		}
	}
	
	public boolean isEmpty()
	{
		return
			this.files.size() == 0 &&
			this.subdirectories.size() == 0;
	}

	public Map<String, ManifestFileInfo> getFiles()
	{
		return files;
	}

	public Map<String, ManifestDirectoryInfo> getSubdirectories()
	{
		return subdirectories;
	}

	private Map<String , ManifestFileInfo> files;
	private Map<String , ManifestDirectoryInfo> subdirectories;
}
