package org.witness.informa.io;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONObject;
import org.witness.informa.utils.ExifModifier;
import org.witness.sscphase1.ObscuraApp;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Log;

public class ImageWriter {
	Bitmap _source, _mod;
	String _path, _adHash, _newPath, _aquiredData;
	
	int dim = 300;
	
	public ImageWriter(String path, JSONObject aquiredData) {
		_path = path;
		_source = BitmapFactory.decodeFile(_path);
		_aquiredData = aquiredData.toString();
		
		if(appendAcquiredData(aquiredData)) {
			if(appendWatermark(generateWatermark()))
				_newPath = saveInformaCopy();
		}
	}
	
	public String getNewPath() {
		return _newPath;
	}
	
	public boolean appendAcquiredData(JSONObject aquiredData) {
		// TODO: data is appended to end of jpeg.  sloppy, but for proof-of-concept it's what we do.
		Log.d(ObscuraApp.TAG, "aquired data: " + aquiredData.toString());
		
		// same data is hashed for the watermark in SHA-512 (384, 256?)...
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-512");
			md.update(aquiredData.toString().getBytes());
			byte[] b = md.digest();
			
			StringBuffer sb = new StringBuffer();
			for(int i=0;i<b.length;i++)
				sb.append(b[i]);
			
			_adHash = sb.toString();
			Log.d(ObscuraApp.TAG, "and the hash: " + _adHash);
			return true;
		} catch (NoSuchAlgorithmException e) {
			Log.d(ObscuraApp.TAG, "ImageWriterError: " + e);
			return false;
		}
	}
	
	private boolean appendWatermark(BitMatrix watermark) throws NullPointerException {
		_mod = _source.copy(_source.getConfig(), true);
		_source.recycle();
		
		int w = _mod.getWidth();
		int h = _mod.getHeight();
		
		// get the top-left corner of where the bitmap will be
		int watermarkLeft = w - watermark.width;
		int watermarkTop = h - watermark.height;
		
		int x, y;
		x = y = 0;
		
		// copy bitmap with watermark inserted
		for(int row=0;row<h;row++) {
			boolean advance = false;
			y = 0;
			
			for(int col=0;col<w;col++) {				
				if(row >= watermarkTop && col >= watermarkLeft) {
					_mod.setPixel(col, row, watermark.get(x,y) ? 0xFF000000 : 0xFFFFFFFF);
					advance = true;
				}
				if(advance) y++;
			}
			if(advance) x++;
		}
		
		return true;
	}
	
	private BitMatrix generateWatermark() {
		// generate watermark from hash
		Charset charset = Charset.forName("ISO-8859-1");
		CharsetEncoder encoder = charset.newEncoder();
		byte[] b = null;
		
		try {
			
			ByteBuffer bb = encoder.encode(CharBuffer.wrap(_adHash));
			b = bb.array();			
			String data = new String(b, "ISO-8859-1");
			

			
			BitMatrix matrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, dim, dim);
			
			return matrix;
			
		} catch(UnsupportedEncodingException e) {
			Log.d(ObscuraApp.TAG, "ImageWriterError: " + e);
			return null;
		} catch (WriterException e) {
			Log.d(ObscuraApp.TAG, "ImageWriterError: " + e);
			return null;
		} catch (CharacterCodingException e) {
			Log.d(ObscuraApp.TAG, "ImageWriterError: " + e);
			return null;
		}
		
	}
	
	private String saveInformaCopy() {
		FileOutputStream copy;
		try {
			String newPath = _path.substring(0,_path.length() - 4) + "_informa.jpg";
			//String newPath = "/mnt/sdcard/DCIM/Camera/informa/" + new Date().getTime() + "_informa.jpg";
			copy = new FileOutputStream(new File(newPath));
			_mod.compress(CompressFormat.JPEG, 100, copy);
			copy.close();
			_mod.recycle();
			Log.d(ObscuraApp.TAG, "********************** INFORMA IMAGE SAVED AS: " + newPath);
			
			ExifModifier ei = new ExifModifier(newPath);
			ei.addHash(_adHash);
			ei.addOffset(new File(newPath).length());
			ei.zipExifData();
			
			FileOutputStream iFile = new FileOutputStream(newPath, true);
			DataOutputStream dos = new DataOutputStream(iFile);
			dos.writeBytes(_aquiredData);
			dos.flush();
			dos.close();
			
			return newPath;
		} catch (FileNotFoundException e) {
			Log.d(ObscuraApp.TAG, "ImageWriterError: " + e);
			return null;
		} catch (IOException e) {
			Log.d(ObscuraApp.TAG, "ImageWriterError: " + e);
			return null;
		}
		
		// TODO: and inserted into EXIF model tag (once again, sloppy)
				
	}
}
