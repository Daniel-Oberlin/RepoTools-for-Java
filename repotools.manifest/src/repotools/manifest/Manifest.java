package repotools.manifest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.Normalizer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import repotools.utilities.CryptUtilities;
import repotools.utilities.TempDirUtilities;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


public class Manifest
{
	public Manifest()
	{
		guid = UUID.randomUUID();
		rootDirectory = new ManifestDirectoryInfo(".", null);
		ignoreList = new ArrayList<String>();
		inceptionDateUtc = new Date();
		
		// TBD: Might consider actually setting this to something
		defaultHashMethod = null;	
	}
	
	public static Manifest makeCleanManifest()
	{
		// Default implementation when there is no prototype
		Manifest manifest = new Manifest();

		manifest.defaultHashMethod =
			CryptUtilities.defaultHashType;

		// Ignore the manifest file itself
		manifest.ignoreList.add(
			"^" +
			escapeForRegexPattern(Manifest.defaultManifestStandardFilePath) +
			"$");

		// This temporary directory is only used by RepoSync
		String tempDirectoryStandardPath = "." +
			standardPathDelimiterString +
			TempDirUtilities.tempDirectoryName +
			standardPathDelimiterString;

		// Add this to ignore temp files if RepoSync operation is interrupted
		manifest.ignoreList.add(
			"^" +
			escapeForRegexPattern(tempDirectoryStandardPath));

		return manifest;
	}

	public Manifest(Manifest original)
	{
		guid = original.guid;
		inceptionDateUtc = original.inceptionDateUtc;
		lastUpdateDateUtc = original.lastUpdateDateUtc;
	
		rootDirectory = new ManifestDirectoryInfo(
				original.rootDirectory, null);
	
		copyManifestInfoFrom(original);
	}

	public void copyManifestInfoFrom(Manifest other)
	{
		name = other.name;
		description = other.description;
		manifestInfoLastModifiedUtc = other.manifestInfoLastModifiedUtc;
		ignoreList = new ArrayList<String>(other.ignoreList);
		defaultHashMethod = other.defaultHashMethod;
	}

	public Manifest cloneFromPrototype()
	{
		// Make a copy
		Manifest clone = new Manifest(this);
		
		// Except for...
		clone.guid = UUID.randomUUID();
		clone.setInceptionDateUtc(new Date());
		clone.setLastUpdateDateUtc(clone.getInceptionDateUtc());
		clone.setManifestInfoLastModifiedUtc(clone.getInceptionDateUtc());
		
		return clone;
	}

	public static Manifest readManifestFile(String manifestFilePath)
		throws IOException
	{		
		BufferedReader reader = new BufferedReader(
			new FileReader(manifestFilePath));

		Gson gson = makeGson();
		Manifest manifest = gson.fromJson(reader, Manifest.class);
		
		// We don't serialize references back to the parent because it causes
		// cycles in the graph which can't be traversed by gson.
		manifest.setParentDirectories(manifest.getRootDirectory());
		
		manifest.manifestFile = new File(manifestFilePath);
		
		return manifest;
	}
	
	protected void setParentDirectories(ManifestDirectoryInfo thisDir)
	{
		for (ManifestFileInfo nextFile :
			thisDir.getFiles().values())
		{
			nextFile.setParentDirectory(thisDir);
		}
		
		for (ManifestDirectoryInfo nextDir :
			thisDir.getSubdirectories().values())
		{
			nextDir.setParentDirectory(thisDir);
			setParentDirectories(nextDir);
		}
	}
	
	public void writeManifestFile(String manifestFilePath)
		throws IOException
	{
		Gson gson = makeGson();
		String json = gson.toJson(this);
		
		FileWriter writer = new FileWriter(manifestFilePath);
		writer.write(json);
		writer.close();
	}
	
	/// <summary>
	/// Count the number of files in the manifest
	/// </summary>
	/// <returns>
	/// The number of files in the manifest
	/// </returns>
	public long countFiles()
	{
		return countFilesRecursive(rootDirectory);
	}

	/// <summary>
	/// Recursive helper function to count the files
	/// </summary>
	/// <param name="currentDir">
	/// Current directory in the recursion
	/// </param>
	/// <returns>
	/// Number of files below the current directory
	/// </returns>
	protected long countFilesRecursive(ManifestDirectoryInfo currentDir)
	{
		long fileCount = currentDir.getFiles().size();
		
		for (ManifestDirectoryInfo nextDirInfo :
			currentDir.getSubdirectories().values())
		{
			fileCount += countFilesRecursive(nextDirInfo);
		}
		
		return fileCount;
	}
	
	/// <summary>
	/// Count the number of bytes stored in the repository
	/// </summary>
	/// <returns>
	/// The number of bytes stored in the repository
	/// </returns>
	public long countBytes()
	{
		return countBytesRecursive(rootDirectory);
	}

	/// <summary>
	/// Recursive helper to count the number of bytes
	/// </summary>
	/// <param name="currentDir">
	/// Current directory in the recursion
	/// </param>
	/// <returns>
	/// The number of bytes stored in the current directory
	/// </returns>
	protected long countBytesRecursive(ManifestDirectoryInfo currentDir)
	{
		long byteCount = 0;
		
		for (ManifestFileInfo nextFileInfo :
			currentDir.getFiles().values())
		{
			byteCount += nextFileInfo.getFileLength();
		}
		
		for (ManifestDirectoryInfo nextDirInfo :
			currentDir.getSubdirectories().values())
		{
			byteCount += countBytesRecursive(nextDirInfo);
		}
		
		return byteCount;
	}
	
	private static Gson makeGson()
	{
		return new GsonBuilder()
			.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
			.registerTypeHierarchyAdapter(byte[].class, new ByteArrayToBase64TypeAdapter())
			.registerTypeAdapter(Date.class, new DateTypeAdapter())
			.create();
	}
	
	private static class ByteArrayToBase64TypeAdapter
		implements JsonSerializer<byte[]>, JsonDeserializer<byte[]>
	{
		public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException
		{
			try
			{
				return Base64.decode(json.getAsString(), Base64.DONT_GUNZIP);
			}
			catch (IOException e)
			{
				// TODO: Throw serialization error?
				return null;
			}
		}
	
		public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context)
		{
			return new JsonPrimitive(Base64.encodeBytes(src));
		}
	}
	
	// Code below works around a bug in GSON where it will only serialize date
	// to local timezone, and not in UTC.
	///
	// GSON default date serializer is locale-specific
	// https://github.com/google/gson/issues/281
	private static class DateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date>
	{
		private final DateFormat dateFormat;
		
		private DateTypeAdapter()
		{
			dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		}
		
		@Override public synchronized JsonElement serialize(Date date, Type type,
		JsonSerializationContext jsonSerializationContext)
		{
			return new JsonPrimitive(dateFormat.format(date));
		}
		
		@Override public synchronized Date deserialize(JsonElement jsonElement, Type type,
		JsonDeserializationContext jsonDeserializationContext)
		{
			try
			{
				return dateFormat.parse(jsonElement.getAsString());
			}
			catch (ParseException e)
			{
				throw new JsonParseException(e);
			}
		}
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public Date getInceptionDateUtc()
	{
		return inceptionDateUtc;
	}

	public void setInceptionDateUtc(Date inceptionDateUtc)
	{
		this.inceptionDateUtc = inceptionDateUtc;
	}

	public Date getLastUpdateDateUtc()
	{
		return lastUpdateDateUtc;
	}

	public void setLastUpdateDateUtc(Date lastUpdateDateUtc)
	{
		this.lastUpdateDateUtc = lastUpdateDateUtc;
	}

	public Date getLastValidateDateUtc()
	{
		return lastValidateDateUtc;
	}

	public void setLastValidateDateUtc(Date lastValidateDateUtc)
	{
		this.lastValidateDateUtc = lastValidateDateUtc;
	}

	public Date getManifestInfoLastModifiedUtc()
	{
		return manifestInfoLastModifiedUtc;
	}

	public void setManifestInfoLastModifiedUtc(Date manifestInfoLastModifiedUtc)
	{
		this.manifestInfoLastModifiedUtc = manifestInfoLastModifiedUtc;
	}

	public String getDefaultHashMethod()
	{
		return defaultHashMethod;
	}

	public void setDefaultHashMethod(String defaultHashMethod)
	{
		this.defaultHashMethod = defaultHashMethod;
	}

	public UUID getGuid()
	{
		return guid;
	}

	public ManifestDirectoryInfo getRootDirectory()
	{
		return rootDirectory;
	}

	public ArrayList<String> getIgnoreList()
	{
		return ignoreList;
	}
	
	private File manifestFile;
	public File getManifestFile()
	{
		return manifestFile;
	}

	private UUID guid;
	private ManifestDirectoryInfo rootDirectory;
	private String name;
	public String description;
	public Date inceptionDateUtc;
	private Date lastUpdateDateUtc;
	private Date lastValidateDateUtc;
	private Date manifestInfoLastModifiedUtc;
	private ArrayList<String> ignoreList;
	private String defaultHashMethod;
	
	
	// Static
	
	/// <summary>
	/// Static initializer
	/// </summary>
	static
	{
		String standardPathDelimiterStringTemp = "/";
		String defaultManifestFileNameTemp = ".repositoryManifest";
		
		// Awkward because Java won't let you reference these
		// directly in a static initializer because it still considers
		// them to be undefined.
		standardPathDelimiterString = standardPathDelimiterStringTemp;
		defaultManifestFileName = defaultManifestFileNameTemp;
		
		defaultManifestStandardFilePath = "." +
			standardPathDelimiterStringTemp +
			defaultManifestFileNameTemp;
	
		// Tolerate up to two seconds of difference
		filesystemDateToleranceMilliseconds = 2000;
		
		// Tolerate up to one millisecond of difference
		manifestDateToleranceMilliseconds = 1;
	}

	/// <summary>
	/// Make a standard UNIX-style relative path, which will not vary
	/// across platforms.
	/// </summary>
	/// <param name="fileInfo">
	/// The file whose path will be generated
	/// </param>
	/// <returns>
	/// The path
	/// </returns>
	public static String makeStandardPathString(ManifestFileInfo fileInfo)
	{
		return makeStandardPathString(fileInfo.getParentDirectory()) + fileInfo.getName();
	}

	/// <summary>
	/// Make a standard UNIX-style relative path, which will not vary
	/// across platforms.
	/// </summary>
	/// <param name="directoryInfo">
	/// The directory whose path will be generated
	/// </param>
	/// <returns>
	/// The path
	/// </returns>
	public static String makeStandardPathString(ManifestDirectoryInfo directoryInfo)
	{
		String pathString = directoryInfo.getName() + standardPathDelimiterString;
		
		if (directoryInfo.getParentDirectory() != null)
		{
			pathString = makeStandardPathString(directoryInfo.getParentDirectory()) + pathString;
		}
		
		return pathString;
	}

	/// <summary>
	/// Make a platform-specific relative path
	/// </summary>
	/// <param name="fileInfo">
	/// The file or directory whose path will be generated
	/// </param>
	/// <returns>
	/// The path
	/// </returns>
	public static String makeNativePathString(ManifestObjectInfo objInfo)
	{
		// Normalize returns a canonical string so that Unicode strings
		// with international characters are always the same and can be
		// compared even if the were originally encoded differently.
		//
		// We use form C because that's what we chose with the original
		// .NET version because it is the default for that platform.
		return Normalizer.normalize(
			objInfo.getFileObject().getAbsolutePath(),
			Normalizer.Form.NFC);	
	}

	// When copying NTFS files over to OSX, "last modified" dates can
	// be slightly different up to almost 1 second.  It seems like many
	// smaller files get the date copied exactly.  For the other files,
	// it almost seems like any precision higher than 1 second is ignored
	// because the time differences are uniformly and randomly distributed
	// between 0s and 1s.  So we choose a small tolerance and allow for
	// the dates to vary slightly from those recorded in the manifest.
	//
	// Further note, I had to increase the tolerance to 2s because of
	// difficulties maintaining consistency with "last modified dates" of
	// encrypted files.  It seems that we get a higher precision time when
	// we get the FileInfo object immediately after we write the file -
	// with precision at 1ms.  Then later when we query the file again, we
	// see a precision of 1s.  So we use a 2s tolerance to account for +/-
	// 1s.  These observations were while using the exFAT format, so not
	// sure to what extent that makes a difference.
	public static boolean compareManifestDateToFilesystemDate(Date date1, Date date2)
	{
		if (Math.abs(date1.getTime() - date2.getTime()) >
			filesystemDateToleranceMilliseconds)
		{
			return false;
		}
		
		return true;
	}

	// Restrict precision when comparing DateTimes stored in manifest
	// because JSON serialization implementation only stores times with
	// millisecond precision.
	public static boolean compareManifestDates(Date date1, Date date2)
	{
		if (Math.abs(date1.getTime() - date2.getTime()) >
			manifestDateToleranceMilliseconds)
		{
			return false;
		}
		
		return true;
	}
	
	public static String escapeForRegexPattern(String originalString)
	{
		String escapedString = "";
		String escapeThese = ".()[]$^";

		for (int i = 0; i < originalString.length(); i++)
		{
			char nextChar = originalString.charAt(i);
			
			if (escapeThese.indexOf(nextChar) >= 0)
			{
				escapedString += '\\';
			}
			
			escapedString += nextChar;
		}
		
		return escapedString;
	}
	
	/// <summary>
	/// The default file path for a manifest
	/// </summary>
	public static String defaultManifestFileName;
	
	/// <summary>
	/// The default file path for a manifest
	/// </summary>
	public static String defaultManifestStandardFilePath;
	
	/// <summary>
	/// The path delimiter string for standard pathnames
	/// </summary>
	public static String standardPathDelimiterString;
	
	/// <summary>
	/// Allowance for difference between DateTime as stored in filesystem
	/// vs manifest.
	/// </summary>
	protected static long filesystemDateToleranceMilliseconds;
	
	/// <summary>
	/// Allowance for difference between DateTime as stored in different
	/// manifests.
	/// </summary>
	protected static long manifestDateToleranceMilliseconds;
}
