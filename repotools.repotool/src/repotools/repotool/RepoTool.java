package repotools.repotool;

import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import repotools.manifest.FileHash;
import repotools.manifest.HashFileDict;
import repotools.manifest.Manifest;
import repotools.manifest.ManifestDirectoryInfo;
import repotools.manifest.ManifestFileInfo;
import repotools.manifest.MovedFileSet;
import repotools.utilities.Console;
import repotools.utilities.CryptUtilities;

public class RepoTool
{
	public RepoTool()
	{
		fileCheckedCount = 0;
		
		showProgress = false;
		update = false;
		alwaysCheckHash = false;
		makeNewHash = false;
		backDate = false;
		trackMoves = false;
		trackDuplicates = false;
		
		newFiles = new ArrayList<ManifestFileInfo>();
		newFilesForGroom = new ArrayList<File>();
		changedFiles = new ArrayList<ManifestFileInfo>();
		missingFiles = new ArrayList<ManifestFileInfo>();
		lastModifiedDateFiles = new ArrayList<ManifestFileInfo>();
		errorFiles = new ArrayList<ManifestFileInfo>();
		ignoredFiles = new ArrayList<ManifestFileInfo>();
		newlyIgnoredFiles = new ArrayList<ManifestFileInfo>();
		ignoredFilesForGroom = new ArrayList<File>();
		movedFiles = new HashMap<FileHash,MovedFileSet>();
		movedFileOrder = new ArrayList<FileHash>();
		duplicateFiles = new HashMap<FileHash, ArrayList<ManifestFileInfo>>();
	}

	public void clear()
	{
		fileCheckedCount = 0;
		
		newFiles.clear();
		newFilesForGroom.clear();
		changedFiles.clear();
		missingFiles.clear();
		lastModifiedDateFiles.clear();
		errorFiles.clear();
		ignoredFiles.clear();
		newlyIgnoredFiles.clear();
		ignoredFilesForGroom.clear();
		movedFiles.clear();
		movedFileOrder.clear();
		duplicateFiles.clear();
	}
	
	public void doUpdate()
	{
		clear();
		
		updateRecursive(
			rootDirectory,
			manifest.getRootDirectory());
		
		if (trackMoves == true)
		{
			doTrackMoves();
		}
		
		if (trackDuplicates == true)
		{
			doTrackDuplicates();
		}
		
		manifest.setLastUpdateDateUtc(new Date());
	}

	protected void updateRecursive(
		File currentDirectoryInfo,
		ManifestDirectoryInfo currentManfestDirInfo)
	{
		// Setup data for current directory as it exists in the file system,
		// and attempt to load all of the files and sub-directories for this
		// directory into these maps.
		Map<String, File> fileDict =
			new HashMap<String, File>();
		
		Map<String, File> dirDict =
			new HashMap<String, File>();
	
		if (currentDirectoryInfo != null)
		{
			File[] fileList = null;
			
			try
			{
				fileList = currentDirectoryInfo.listFiles();
			}
			catch (Exception ex)
			{
				writeLine(Manifest.makeStandardPathString(
					currentManfestDirInfo));
				
				if (ignoreFile(Manifest.makeStandardPathString(
					currentManfestDirInfo)) == true)
				{
					// This was implemented primarily to allow the user to
					// silence the process of skipping over inaccessible
					// system directories by ignoring them.  For example,
					// in some cases the "$RECYCLE BIN" under Windows
					// is not accessible and will generate an error.  The
					// user can now add such directories to the ignore list
					// and they will be silently ignored.  The special
					// message for showProgress alerts the user that the
					// directory is actually being skipped altogether
					// since it can't be accessed.  The only significant
					// implication of this is that the ignored files won't
					// be enumerated and counted as being ignored.
					if (showProgress)
					{
						writeLine(
							Manifest.makeStandardPathString(currentManfestDirInfo) +
							" [IGNORED DIRECTORY AND CANNOT ACCESS]");
					}
				}
				else
				{
					forceWriteLine("Could not access contents of: " +
						currentDirectoryInfo.getAbsolutePath());
				}
				
				return;
			}

			for (File nextFileInfo : fileList)
			{
				// We use form C because that's what we chose with the original
				// .NET version because it is the default for that platform.
				String normalizedName =
					Normalizer.normalize(
						nextFileInfo.getName(),
						Normalizer.Form.NFC);
				
				if (nextFileInfo.isDirectory())
				{
					dirDict.put(normalizedName, nextFileInfo);
				}
				else
				{
					fileDict.put(normalizedName, nextFileInfo);
				}
			}
		}

		// Clone in case we modify during iteration
		ArrayList<ManifestFileInfo> fileListClone =
			new ArrayList<ManifestFileInfo>(currentManfestDirInfo.getFiles().values());

		// Iterate through existing manifest entries
		for (ManifestFileInfo nextManFileInfo : fileListClone)
		{
			if (showProgress)
			{
				write(Manifest.makeStandardPathString(nextManFileInfo));
			}

			File nextFileInfo = fileDict.get(nextManFileInfo.getName());
			if (nextFileInfo != null)
			{
				fileCheckedCount++;

				if (ignoreFile(Manifest.makeStandardPathString(nextManFileInfo)))
				{
					write(" [NEWLY IGNORED]");

					currentManfestDirInfo.getFiles().remove(
						nextManFileInfo.getName());

					newlyIgnoredFiles.add(nextManFileInfo);
				}
				else if (nextFileInfo.length() != nextManFileInfo.getFileLength() &&
					update == false &&
					alwaysCheckHash == false)
				{
					// Don't compute hash if we aren't doing an update
					write(" [DIFFERENT]");
					changedFiles.add(nextManFileInfo);
				}
				else if (alwaysCheckHash == true ||
					makeNewHash == true ||
					nextManFileInfo.getFileHash() == null ||
					Manifest.compareManifestDateToFilesystemDate(
						new Date(nextFileInfo.lastModified()),
						nextManFileInfo.getLastModifiedUtc()) == false ||
					nextFileInfo.length() != nextManFileInfo.getFileLength())
				{
					FileHash checkHash = null;
					
					Exception exception = null;
					try
					{
						String hashType = manifest.getDefaultHashMethod();
						if (nextManFileInfo.getFileHash() != null)
						{
							hashType = nextManFileInfo.getFileHash().getHashType();
						}

						checkHash = FileHash.computeHash(
							nextFileInfo,
							hashType);
					}
					catch (Exception ex)
					{
						exception = ex;
					}

					if (exception != null)
					{
						writeLine(" [ERROR]");
						writeLine(exception.toString());
						
						errorFiles.add(nextManFileInfo);
					}
					else
					{
						if (nextManFileInfo.getFileHash() == null)
						{
							write(" [NULL HASH IN MANIFEST]");
							changedFiles.add(nextManFileInfo);
						}
						else if (checkHash.equals(nextManFileInfo.getFileHash()) == false)
						{
							write(" [DIFFERENT]");
							changedFiles.add(nextManFileInfo);
						}
						else
						{
							if (Manifest.compareManifestDateToFilesystemDate(
								new Date(nextFileInfo.lastModified()),
									nextManFileInfo.getLastModifiedUtc()) == false)
							{
								write(" [LAST MODIFIED DATE]");
								lastModifiedDateFiles.add(nextManFileInfo);
							
								if (backDate == true)
								{
									nextFileInfo.setLastModified(
										nextManFileInfo.getLastModifiedUtc().getTime());
								}
							}
						}
					}

					FileHash newHash = checkHash;
					if (makeNewHash)
					{
						try
						{
							newHash = FileHash.computeHash(
								nextFileInfo,
								getNewHashType(manifest));
						}
						catch (Exception hashException)
						{
							writeLine(" [ERROR MAKING NEW HASH]");
							writeLine(hashException.toString());
							
							errorFiles.add(nextManFileInfo);
						}
					}

					// Update hash and last modified date accordingly
					nextManFileInfo.setFileHash(newHash);
					
					nextManFileInfo.setLastModifiedUtc(
						new Date(nextFileInfo.lastModified()));
					
					nextManFileInfo.setFileLength(nextFileInfo.length());
				}
				else
				{
					write(" [SKIPPED]");
				}
			}
			else
			{
				write(" [MISSING]");
				currentManfestDirInfo.getFiles().remove(nextManFileInfo.getName());
				missingFiles.add(nextManFileInfo);
			}
			
			writeLine("");
		}

		// Clone in case we modify during iteration
		ArrayList<ManifestDirectoryInfo> directoryListClone =
			new ArrayList<ManifestDirectoryInfo>(
				currentManfestDirInfo.getSubdirectories().values());

		for (ManifestDirectoryInfo nextManDirInfo :
			directoryListClone)
		{
			File nextDirInfo = dirDict.get(nextManDirInfo.getName());
			
			updateRecursive(
				nextDirInfo,
				nextManDirInfo);
			
			if (nextManDirInfo.isEmpty())
			{
				currentManfestDirInfo.getSubdirectories().remove(
					nextManDirInfo.getName());
			}
		}

		// Look for new files
		for (String nextFileName : fileDict.keySet())
		{
			File nextFileInfo = fileDict.get(nextFileName);
			
			if (currentManfestDirInfo.getFiles().containsKey(
				nextFileName) == false)
			{
				ManifestFileInfo newManFileInfo =
					new ManifestFileInfo(
						nextFileName,
						currentManfestDirInfo);
				
				write(Manifest.makeStandardPathString(newManFileInfo));
				
				if (ignoreFile(Manifest.makeStandardPathString(newManFileInfo)))
				{
					ignoredFiles.add(newManFileInfo);
			
					// Don't groom the manifest file!
					try
					{
						if (nextFileInfo.getCanonicalPath().equals(
							manifest.getManifestFile().getCanonicalPath()) == false)
						{
							ignoredFilesForGroom.add(nextFileInfo);
						}
					}
					catch (Exception ex)
					{
						// What to do?
					}

					write(" [IGNORED]");
				}
				else
				{
					fileCheckedCount++;

					boolean checkHash = false;
					if (update == true ||
						alwaysCheckHash == true ||
						trackMoves == true)
					{
						checkHash = true;
					}
					
					Exception exception = null;
					if (checkHash)
					{
						try
						{
							newManFileInfo.setFileHash(
								FileHash.computeHash(
								nextFileInfo,
								getNewHashType(manifest)));
						}
						catch (Exception ex)
						{
							exception = ex;
						}
					}

					if (checkHash && newManFileInfo.getFileHash() == null)
					{
						errorFiles.add(newManFileInfo);
						
						writeLine(" [ERROR]");
						writeLine(exception.toString());
					}
					else
					{
						newFiles.add(newManFileInfo);
						newFilesForGroom.add(nextFileInfo);
						write(" [NEW]");
					}
					
					newManFileInfo.setFileLength(
						nextFileInfo.length());
					
					newManFileInfo.setLastModifiedUtc(
						new Date(nextFileInfo.lastModified()));
					
					newManFileInfo.setRegisteredUtc(
						new Date());
					
					currentManfestDirInfo.getFiles().put(
						nextFileName,
						newManFileInfo);
				}
				
				writeLine("");
			}
		}

		// Recurse looking for new directories
		for (String nextDirName : dirDict.keySet())
		{
			File nextDirInfo = dirDict.get(nextDirName);
			
			if (currentManfestDirInfo.getSubdirectories().containsKey(
				nextDirName) == false)
			{
				ManifestDirectoryInfo nextManDirInfo =
					new ManifestDirectoryInfo(
						nextDirName,
						currentManfestDirInfo);
			
				currentManfestDirInfo.getSubdirectories().put(
					nextDirName,
					nextManDirInfo);
			
				updateRecursive(
					nextDirInfo,
					nextManDirInfo);
			
				if (nextManDirInfo.isEmpty())
				{
					currentManfestDirInfo.getSubdirectories().remove(
						nextDirName);
				}
			}
		}
	}
	
	public boolean isBackDate()
	{
		return backDate;
	}

	public void setBackDate(boolean backDate)
	{
		this.backDate = backDate;
	}

	public boolean isTrackMoves()
	{
		return trackMoves;
	}

	public void setTrackMoves(boolean trackMoves)
	{
		this.trackMoves = trackMoves;
	}

	public boolean isTrackDuplicates()
	{
		return trackDuplicates;
	}

	public void setTrackDuplicates(boolean trackDuplicates)
	{
		this.trackDuplicates = trackDuplicates;
	}

	protected void doTrackMoves()
	{
		// For large number of moved files it's probably faster to
		// rebuild these lists from scratch than to remove many
		// individual items from them.
		ArrayList<ManifestFileInfo> missingFilesUpdated =
			new ArrayList<ManifestFileInfo>();
		
		ArrayList<ManifestFileInfo> newFilesUpdated =
			new ArrayList<ManifestFileInfo>();
		
		// Make files easy to find by their hashcodes
		HashFileDict missingFileDict = new HashFileDict();
		for (ManifestFileInfo missingFile : missingFiles)
		{
			missingFileDict.Add(missingFile);
		}
		
		HashFileDict newFileDict = new HashFileDict();
		for (ManifestFileInfo newFile : newFiles)
		{
			newFileDict.Add(newFile);
		}
		
		// Note which new files are really moved files for later when
		// we rebuild the new files list.
		HashSet<ManifestFileInfo> movedFilesSet =
			new HashSet<ManifestFileInfo>();

		for (ManifestFileInfo checkMissingFile : missingFiles)
		{
			if (newFileDict.getDict().containsKey(checkMissingFile.getFileHash()))
			{
				if (movedFiles.containsKey(checkMissingFile.getFileHash()) == false)
				{
					movedFiles.put(
						checkMissingFile.getFileHash(),
						new MovedFileSet());
				
					movedFileOrder.add(checkMissingFile.getFileHash());
				}
				
				movedFiles.get(checkMissingFile.getFileHash()).getOldFiles().add(checkMissingFile);
				
				if (movedFiles.get(checkMissingFile.getFileHash()).getNewFiles().isEmpty())
				{
					// First time only
					for (ManifestFileInfo nextNewFile :
						newFileDict.getDict().get(checkMissingFile.getFileHash()))
					{
						movedFiles.get(checkMissingFile.getFileHash()).getNewFiles().add(nextNewFile);
					
						// Remember for later rebuild
						movedFilesSet.add(nextNewFile);
					}
				}
			}
			else
			{
				missingFilesUpdated.add(checkMissingFile);
			}
		}

		// Rebuild new file list
		newFilesForGroom.clear();
		for (ManifestFileInfo checkNewFile : newFiles)
		{
			if (movedFilesSet.contains(checkNewFile) == false)
			{
				newFilesUpdated.add(checkNewFile);
				
				String checkNewFilePath =
					Manifest.makeNativePathString(checkNewFile);
				
				File newFile = new File(checkNewFilePath);
				
				try
				{
					if (newFile.getCanonicalPath().equals(
						manifest.getManifestFile().getCanonicalPath()) == false)
					{
						newFilesForGroom.add(newFile);
					}
				}
				catch (Exception ex)
				{
					// What to do?
				}
			}
		}
		
		// Replace with updated lists
		missingFiles = missingFilesUpdated;
		newFiles = newFilesUpdated;
	}
	
	protected void doTrackDuplicates()
	{
		duplicateFiles.clear();
		
		HashMap<FileHash, ArrayList<ManifestFileInfo>> fileDict =
			new HashMap<FileHash, ArrayList<ManifestFileInfo>>();
		
		checkDuplicatesRecursive(
			manifest.getRootDirectory(),
			fileDict);
		
		for (FileHash nextHash : fileDict.keySet())
		{
			ArrayList<ManifestFileInfo> nextList =
				fileDict.get(nextHash);
		
			if (nextList.size() > 1)
			{
				duplicateFiles.put(nextHash, nextList);
			}
		}
	}

	protected void checkDuplicatesRecursive(
		ManifestDirectoryInfo currentDirectory,
		HashMap<FileHash, ArrayList<ManifestFileInfo>> fileDict)
	{
		for (ManifestFileInfo nextFileInfo :
			currentDirectory.getFiles().values())
		{
			if (fileDict.containsKey(nextFileInfo.getFileHash()) == false)
			{
				fileDict.put(
					nextFileInfo.getFileHash(),
					new ArrayList<ManifestFileInfo>());
			}
			
			fileDict.get(nextFileInfo.getFileHash()).add(nextFileInfo);
		}
			
		for (ManifestDirectoryInfo nextDirInfo :
			currentDirectory.getSubdirectories().values())
		{
			checkDuplicatesRecursive(
				nextDirInfo,
				fileDict);
		}
	}

	
	// Helper methods

	public Manifest makeManifest()
	{
		// TODO: Support for manifest prototypes
		return Manifest.makeCleanManifest();
	}

	protected void writeLine(String message)
	{
		write(message + "\r\n");
	}

	protected void write(String message)
	{
		if (showProgress && console != null)
		{
			console.write(message);
		}
	}

	protected void forceWriteLine(String message)
	{
		forceWrite(message + "\r\n");
	}

	protected void forceWrite(String message)
	{
		if (console != null)
		{
			console.write(message);
		}
	}

	protected String getNewHashType(Manifest man)
	{
		if (man.getDefaultHashMethod() != null)
		{
			switch (man.getDefaultHashMethod())
			{
				case "MD5":
				case "SHA256":
					return man.getDefaultHashMethod();
			}
		}

		return NewHashType;
	}

	protected boolean ignoreFile(String fileName)
	{
		for (String nextExpression : manifest.getIgnoreList())
		{
			Pattern pattern = Pattern.compile(nextExpression);
			Matcher matcher = pattern.matcher(fileName);
		
			if (matcher.matches())
			{
				return true;
			}
		}
	
		return false;
	}


	// Data members and accessors

	public Console getConsole()
	{
		return console;
	}

	public void setConsole(Console console)
	{
		this.console = console;
	}
	
	public int getFileCheckedCount()
	{
		return fileCheckedCount;
	}

	public boolean isMakeNewHash()
	{
		return makeNewHash;
	}

	public void setMakeNewHash(boolean makeNewHash)
	{
		this.makeNewHash = makeNewHash;
	}

	public boolean isShowProgress()
	{
		return showProgress;
	}

	public void setShowProgress(boolean showProgress)
	{
		this.showProgress = showProgress;
	}

	private Console console; 
	private File rootDirectory;

	public void setRootDirectory(File rootDirectory)
	{
		this.rootDirectory = rootDirectory;
	}

	private boolean showProgress;
	private boolean update;
	public void setUpdate(boolean update)
	{
		this.update = update;
	}

	private boolean alwaysCheckHash;
	private boolean makeNewHash;
	public boolean backDate;


	public boolean trackMoves;
	public boolean trackDuplicates;

	private int fileCheckedCount;

	public boolean isAlwaysCheckHash()
	{
		return alwaysCheckHash;
	}

	public void setAlwaysCheckHash(boolean alwaysCheckHash)
	{
		this.alwaysCheckHash = alwaysCheckHash;
	}

	private Manifest manifest;
	public Manifest getManifest()
	{
		return manifest;
	}

	public void setManifest(Manifest manifest)
	{
		this.manifest = manifest;
	}

	private ArrayList<ManifestFileInfo> newFiles;
	public ArrayList<ManifestFileInfo> getNewFiles()
	{
		return newFiles;
	}

	private ArrayList<File> newFilesForGroom;
	public ArrayList<File> getNewFilesForGroom()
	{
		return newFilesForGroom;
	}

	public ArrayList<ManifestFileInfo> getChangedFiles()
	{
		return changedFiles;
	}

	private ArrayList<ManifestFileInfo> changedFiles;
	private ArrayList<ManifestFileInfo> missingFiles;
	public ArrayList<ManifestFileInfo> getMissingFiles()
	{
		return missingFiles;
	}

	private ArrayList<ManifestFileInfo> lastModifiedDateFiles;
	public ArrayList<ManifestFileInfo> getLastModifiedDateFiles()
	{
		return lastModifiedDateFiles;
	}

	private ArrayList<ManifestFileInfo> errorFiles;
	public ArrayList<ManifestFileInfo> getErrorFiles()
	{
		return errorFiles;
	}

	public ArrayList<ManifestFileInfo> getIgnoredFiles()
	{
		return ignoredFiles;
	}

	private ArrayList<ManifestFileInfo> ignoredFiles;
	private ArrayList<ManifestFileInfo> newlyIgnoredFiles;
	private ArrayList<File> ignoredFilesForGroom;
	public ArrayList<File> getIgnoredFilesForGroom()
	{
		return ignoredFilesForGroom;
	}

	private Map<FileHash, MovedFileSet> movedFiles;
	public ArrayList<ManifestFileInfo> getNewlyIgnoredFiles()
	{
		return newlyIgnoredFiles;
	}

	public ArrayList<FileHash> getMovedFileOrder()
	{
		return movedFileOrder;
	}

	public Map<FileHash, ArrayList<ManifestFileInfo>> getDuplicateFiles()
	{
		return duplicateFiles;
	}

	public Map<FileHash, MovedFileSet> getMovedFiles()
	{
		return movedFiles;
	}

	private ArrayList<FileHash> movedFileOrder;
	private Map<FileHash, ArrayList<ManifestFileInfo>> duplicateFiles;


	// Static
	
	/// <summary>
	/// Static initializer
	/// </summary>
	static
	{
		// Make this a native path because we only deal with it as a native
		// file and never as part of a repository.
		ManifestNativeFilePath =
			"." +
			File.separator +
			Manifest.defaultManifestFileName;
		
		PrototypeManifestFileName = ".manifestPrototype";
		NewHashType = CryptUtilities.defaultHashType;
	}

	public static String ManifestFileName;
	public static String ManifestNativeFilePath;
	public static String PrototypeManifestFileName;
	public static String NewHashType;
}
