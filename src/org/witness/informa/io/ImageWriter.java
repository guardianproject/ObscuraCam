package org.witness.informa.io;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.witness.sscphase1.ObscuraApp;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import android.graphics.Bitmap;
import android.util.Log;

public class ImageWriter {
	Bitmap _source;
	String _path, _adHash;
	
	public ImageWriter(Bitmap source, String path) {
		_source = source;
		_path = path;
	}
	
	public void appendAcquiredData(String aquiredData) {
		// TODO: data is appended to end of jpeg.  sloppy, but for proof-of-concept it's what we do.
		
		// same data is hashed for the watermark in SHA-512 (384, 256?)...
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-512");
			md.update(aquiredData.getBytes());
			byte[] b = md.digest();
			
			StringBuffer sb = new StringBuffer();
			for(int i=0;i<b.length;i++)
				sb.append(b[i]);
			
			_adHash = sb.toString();
		} catch (NoSuchAlgorithmException e) {
			Log.d(ObscuraApp.TAG, "ImageWriterError: " + e);
		}
	}
	
	private void appendWatermark() {
		// generate watermark from hash
		Charset charset = Charset.forName("ISO-8859-1");
		CharsetEncoder encoder = charset.newEncoder();
		byte[] b = null;
		
		try {
			
			ByteBuffer bb = encoder.encode(CharBuffer.wrap(_adHash));
			b = bb.array();			
			String data = new String(b, "ISO-8859-1");
			
			int h = 100;
			int w = 100;
			
			BitMatrix matrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, w, h);
			int[] pixels = new int[matrix.getWidth() * matrix.getHeight()];
			for(int y=0;y<matrix.getHeight();y++) {
				int offset = y * matrix.getWidth();
				for(int x=0;x<matrix.getWidth();x++) {
					pixels[offset + x] = matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
				}
			}
			
			Bitmap watermark = Bitmap.createBitmap(matrix.getWidth(), matrix.getHeight(), Bitmap.Config.ARGB_8888);
			watermark.setPixels(pixels, 0, matrix.getWidth(), 0, 0, matrix.getWidth(), matrix.getHeight());
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			watermark.compress(Bitmap.CompressFormat.PNG, 50, baos);
			
			// TODO: FROM HERE, WE add the bitmap to the lower right corner of the image...
			
		} catch(UnsupportedEncodingException e) {
			Log.d(ObscuraApp.TAG, "ImageWriterError: " + e);
		} catch (WriterException e) {
			Log.d(ObscuraApp.TAG, "ImageWriterError: " + e);
		} catch (CharacterCodingException e) {
			Log.d(ObscuraApp.TAG, "ImageWriterError: " + e);
		}
		
	}
	
	private void saveImage() {
		// TODO: create as a jpeg image
		
		// TODO: and inserted into EXIF model tag (once again, sloppy)
		
		// TODO: clone with the filetype ".informa"
		
		// TODO: delete temp jpeg
	}
}
