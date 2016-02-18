package repotools.manifest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ManifestConsole extends repotools.utilities.Console
{
	public void detailFiles(
		Iterable<ManifestFileInfo> files)
	{
		if (detail)
		{
			for (ManifestFileInfo nextManFileInfo : files)
			{
				writeLine("   " + Manifest.makeStandardPathString(nextManFileInfo));
			}
			
			writeLine();
		}
	}

	public void detailFiles(
		List<FileHash> movedFileOrder,
		Map<FileHash, MovedFileSet> movedFileSets,
		boolean reverseOrder)
	{
		if (detail)
		{
			for (FileHash nextHash : movedFileOrder)
			{
				write("   ");
				MovedFileSet nextFileSet = movedFileSets.get(nextHash);
				
				ArrayList<ManifestFileInfo> leftSide =
				nextFileSet.getOldFiles();
				
				ArrayList<ManifestFileInfo> rightSide =
				nextFileSet.getNewFiles();
				
				if (reverseOrder)
				{
					ArrayList<ManifestFileInfo> temp = leftSide;
					leftSide = rightSide;
					rightSide = temp;
				}
				
				for (ManifestFileInfo nextOldFile : leftSide)
				{
					write(Manifest.makeStandardPathString(nextOldFile));
					write(" ");
				}
				
				write("->");
				
				for (ManifestFileInfo nextNewFile : rightSide)
				{
					write(" ");
					write(Manifest.makeStandardPathString(nextNewFile));
				}
				writeLine();
			}
			
			writeLine();
		}
	}

	public void detailFiles(
		Map<FileHash, ArrayList<ManifestFileInfo>> files)
	{
		if (detail)
		{
			for (FileHash nextHash : files.keySet())
			{
				writeLine("   " + nextHash.toString());
				
				for (ManifestFileInfo nextFile : files.get(nextHash))
				{
					writeLine("      " + Manifest.makeStandardPathString(nextFile));
				}
				writeLine();
			}

			writeLine();
		}
	}
}
