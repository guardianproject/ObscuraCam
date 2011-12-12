package org.witness.informa.utils.secure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MediaHasher 
{
	private final static int BYTE_READ_SIZE = 8192;

	public static String hash (File file, String hashFunction)  throws IOException, NoSuchAlgorithmException
	{
		return hash (new FileInputStream(file), hashFunction);
	}
	
	public static String hash (InputStream is, String hashFunction) throws IOException, NoSuchAlgorithmException
	{
		MessageDigest digester;
		
		digester = MessageDigest.getInstance(hashFunction); //MD5 or SHA-1
	
		  byte[] bytes = new byte[BYTE_READ_SIZE];
		  int byteCount;
		  while ((byteCount = is.read(bytes)) > 0) {
		    digester.update(bytes, 0, byteCount);
		  }
		  
		  byte[] messageDigest = digester.digest();
		  
		// Create Hex String
	        StringBuffer hexString = new StringBuffer();
	        for (int i=0; i<messageDigest.length; i++)
	            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
	        return hexString.toString();
	
	}
}