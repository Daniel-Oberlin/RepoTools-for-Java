package repotools.manifest;

import java.util.Date;

public class ManifestFileInfo extends ManifestObjectInfo
{
	public ManifestFileInfo(
		String name,
		ManifestDirectoryInfo parentDirectory)
	{
		super(name, parentDirectory);
		this.fileHash = null;
	}

	public ManifestFileInfo(
		ManifestFileInfo original,
		ManifestDirectoryInfo parentDirectory)
	{
			super(original.getName(), parentDirectory);
			this.fileLength = original.fileLength;
			this.lastModifiedUtc = original.lastModifiedUtc;
			this.registeredUtc = original.registeredUtc;
			this.fileHash = original.fileHash;
	}

	public long getFileLength()
	{
		return fileLength;
	}
	public void setFileLength(long fileLength)
	{
		this.fileLength = fileLength;
	}
	public Date getLastModifiedUtc()
	{
		return lastModifiedUtc;
	}
	public void setLastModifiedUtc(Date lastModifiedUtc)
	{
		this.lastModifiedUtc = lastModifiedUtc;
	}
	public Date getRegisteredUtc()
	{
		return registeredUtc;
	}
	public void setRegisteredUtc(Date registeredUtc)
	{
		this.registeredUtc = registeredUtc;
	}
	public FileHash getFileHash()
	{
		return fileHash;
	}
	public void setFileHash(FileHash fileHash)
	{
		this.fileHash = fileHash;
	}

	private long fileLength;
	private Date lastModifiedUtc;
	private Date registeredUtc;
	private FileHash fileHash;
}
