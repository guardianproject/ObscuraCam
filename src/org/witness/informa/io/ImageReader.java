package org.witness.informa.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Hashtable;

import org.json.JSONException;
import org.json.JSONObject;
import org.witness.informa.utils.ExifModifier;
import org.witness.sscphase1.ObscuraApp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.decoder.Decoder;

public class ImageReader {
	boolean _isValid = false;
	ArrayList<JSONObject> _imageRegions;
	String _globalParams, _path;
	
	BitMatrix _watermark;
	Bitmap _source, _qrBmp;
	
	byte[] _qrBytes;
	
	int dim = 300;
	
	ExifModifier _ei;
	
	public ImageReader(String path) {
		// TODO: extract watermark
		_path = path;
		_source = BitmapFactory.decodeFile(_path);
		
		int watermarkLeft = _source.getWidth() - dim;
		int watermarkTop = _source.getHeight() - dim;
		
		_qrBmp = Bitmap.createBitmap(_source, watermarkLeft, watermarkTop, dim, dim);
		
		BitMatrix _watermark = new BitMatrix(dim);
		int[] pixels = new int[_qrBmp.getWidth() * _qrBmp.getHeight()];
		_qrBmp.getPixels(pixels, 0, _qrBmp.getWidth(), 0, 0, _qrBmp.getWidth(), _qrBmp.getHeight());
		
		//Log.d(ObscuraApp.TAG, "there are " + _qrBytes.length + " bytes");
		//Log.d(ObscuraApp.TAG, "there are ");
		
		int x, y, p;
		x = y = p = 0;
		
		for(int row = 0; row < _watermark.height; row++) {
			y = 0;
			for(int col = 0; col < _watermark.width; col++) {
				//Log.d(ObscuraApp.TAG, "pixel: " + pixels[p]);
				if(pixels[p] > -100)
					_watermark.set(x, y);
				y++;
				p++;
			}
			x++;
		}
		
		Log.d(ObscuraApp.TAG, "matrix: " + _watermark.toString());
		
		
		
		
		/*
		FileOutputStream fos;
		try {
			fos = new FileOutputStream("/mnt/sdcard/DCIM/Camera/informa/tempqr.jpg");
			_qrBmp.compress(Bitmap.CompressFormat.PNG, 50, fos);
			fos.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
		
		//_qrBmp = BitmapFactory.decodeFile("/mnt/sdcard/DCIM/Camera/informa/test-qr.jpg");
		
		// TODO: decode it UGH THIS IS NOT WORKING.
		Decoder decoder = new Decoder();
		Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>();
		hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
		
		LuminanceSource ls = new RGBLuminanceSource(_qrBmp);
		BinaryBitmap bb = new BinaryBitmap(new HybridBinarizer(ls));
		Reader reader = new QRCodeReader();
		
		try {
			DecoderResult result = decoder.decode(_watermark, hints);
			Result result2 = reader.decode(bb);
			Log.d(ObscuraApp.TAG,"hooray! : " + result.getText());
			Log.d(ObscuraApp.TAG,"hooray! : " + result2.getText());
			
		} catch (ChecksumException e) {
			Log.d(ObscuraApp.TAG,"hmm: " + e);
			
		} catch (FormatException e) {
			Log.d(ObscuraApp.TAG,"hmm: " + e + "\nTrying to decode the bitmap?");
			
			
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		// TODO: check it against exif model tag
		_ei = new ExifModifier(_path);
		try {
			Log.d(ObscuraApp.TAG, "DATA OFFSET: " + _ei.getExifTags("InformaCam_SOI"));
			Log.d(ObscuraApp.TAG, "CHECKSUM: " + _ei.getExifTags(ExifInterface.TAG_MODEL));
		} catch (JSONException e) {
			Log.d(ObscuraApp.TAG,"hmm: " + e);
		}
		
		/*
		 *  TODO: if it's a match, unpack the data at the end of the jpeg
		 *  
		 *  _globalParams = whatever the intent & geneaology data contains
		 *  _imageRegions = whatever image regions encoded therein
		 */
		
	}
	
	public Bitmap getQrBitmap() {
		return _qrBmp;
	}
	
	public String getParams() {
		return _globalParams;
	}
	
	public ArrayList<JSONObject> getImageRegions() {
		return _imageRegions;
	}
	
	public boolean isValid() {
		return _isValid;
	}
}
