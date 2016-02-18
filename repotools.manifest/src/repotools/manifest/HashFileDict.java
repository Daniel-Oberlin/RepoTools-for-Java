package repotools.manifest;

import java.util.ArrayList;
import java.util.HashMap;


public class HashFileDict
{
	public HashFileDict()
	{
		dict = new HashMap<FileHash, ArrayList<ManifestFileInfo>>();
	}
	
	public void Add(ManifestFileInfo manFileInfo)
	{
		if (dict.containsKey(manFileInfo.getFileHash()) == false)
		{
			dict.put(manFileInfo.getFileHash(), new ArrayList<ManifestFileInfo>());
		}
	
		dict.get(manFileInfo.getFileHash()).add(manFileInfo);
	}

	private HashMap<FileHash, ArrayList<ManifestFileInfo>> dict;

	public HashMap<FileHash, ArrayList<ManifestFileInfo>> getDict()
	{
		return dict;
	}
}
