package repotools.manifest;

import java.io.File;
import java.util.Arrays;

import repotools.utilities.CryptUtilities;

public class FileHash
{
	public FileHash(byte[] hashData, String hashType)
	{
		this.hashData = hashData;
		this.hashType = hashType;
	}

	@Override
	public int hashCode()
	{
		// Not serialized so we regenerate on the fly
		if (objectHashIsSet == false)
		{
			// Take first 4 bytes of hash data for object hash
			objectHash = 0;
			for (int i = 0; i < 4; i++)
			{
				objectHash <<= 8;
				objectHash |= hashData[i];
			}
			objectHashIsSet = true;
		}

		return objectHash;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
		{
			return true;
		}
		
		if ((obj instanceof FileHash) == false)
		{
			return false;
		}
		
		FileHash other = (FileHash) obj;
		
		if (other.hashType != hashType)
		{
			return false;
		}
		
		return Arrays.equals(hashData, other.hashData);
	}

	@Override
	public String toString()
	{
		return javax.xml.bind.DatatypeConverter.printHexBinary(hashData);
	}

	private String hashType;
	public String getHashType()
	{
		return hashType;
	}

	private byte[] hashData;
	
	private transient int objectHash;
	private transient boolean objectHashIsSet;


	// Static
	
	static public FileHash computeHash(
		File file,
		String hashType) throws Exception
	{
	return new FileHash(
		CryptUtilities.computeHash(file, hashType),
		hashType);
	}
}
