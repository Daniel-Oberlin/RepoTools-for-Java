package repotools.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;


public class CryptUtilities
{
	static public byte[] computeHash(
		File file,
		String hashType) throws Exception
	{
		return computeHash(
			new FileInputStream(file),
			hashType);
	}
	
	static public byte[] computeHash(
		InputStream stream,
		String hashType) throws Exception
	{
		MessageDigest digest = getHashAlgorithm(hashType);
	
		byte[] buffer = new byte[1024];
		int numRead;
	
		do
		{
			numRead = stream.read(buffer);
			
			if (numRead > 0)
			{
				digest.update(buffer, 0, numRead);
			}
			
		} while (numRead != -1);
	
		stream.close();
		return digest.digest();
	}

	static public MessageDigest getHashAlgorithm(
		String hashType) throws Exception
	{
		// Convert from .NET to Java names
		switch (hashType)
		{
			case "SHA256":
				hashType = "SHA-256";
				break;
			 
			case "MD5":
				break;
		 		
		 default:
			 // Only support above two for right now
			 throw new Exception("Unrecognized hash method: " + hashType);
		}
		
		MessageDigest digest =
			MessageDigest.getInstance(hashType);
		
		return digest;
	}

	// I think I chose MD5 because SHA256 was less available on Mono,
	// and MD5 is faster to compute.
	public static final String defaultHashType = "MD5";
}
