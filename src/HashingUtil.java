import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashingUtil {
	static private String bytesToHexString(byte[] bytes) {
		// Convert from byte[] to String
		StringBuffer sb = new StringBuffer();
		for (byte b : bytes) {
			int i = 0xFF & b;
			if (i < 0x10)
				sb.append('0');
			sb.append(Integer.toHexString(i));
		}
		return sb.toString();
	}
	
	static private MessageDigest getMessageDigestInstance()
	{
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return md;
	}

	static public String hash( String s ) {
		MessageDigest md = getMessageDigestInstance();
		md.update( s.getBytes() );
		return bytesToHexString( md.digest() );
	}
}
