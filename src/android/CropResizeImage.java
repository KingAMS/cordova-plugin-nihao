package com.nihaolabs.nihao.cordova;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.util.DisplayMetrics;

public class CropResizeImage implements Runnable {
    public static final String FORMAT_JPG = "jpg";
    public static final String FORMAT_PNG = "png";
    public static final String DEFAULT_FORMAT = "jpg";
	
	
	protected JSONObject params;
	protected CallbackContext callbackContext;
	protected int width;
	protected int height;
	protected String format;
	protected String imageUri;

	public CropResizeImage(JSONObject params, CallbackContext callbackContext) throws JSONException {
		this.params = params;
		this.callbackContext = callbackContext;
		this.imageUri = params.getString("imageUri");
		this.width = params.getInt("width");
		this.height = params.getInt("height");
		this.format = "jpg";
		if (params.has("format")) {
			this.format = params.getString("format");
		}
	}

	@Override
	public void run() {
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			Bitmap bmp = getBitmap(this.imageUri , options);
			if (bmp == null) {
				throw new IOException("The image file could not be opened.("+this.imageUri+")");
			}

			bmp = scaleCropToFit(bmp,this.width,this.height);

			int quality = params.getInt("quality");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if (format.equals("png")) {
				bmp.compress(Bitmap.CompressFormat.PNG, quality, baos);
			} else {
				bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos);
			}
			byte[] b = baos.toByteArray();
			String returnString = Base64.encodeToString(b, Base64.NO_WRAP);
			if (params.getBoolean("storeImage")) {
				storeImage(params, format, bmp, callbackContext);
			} else {
				// return object
				JSONObject res = new JSONObject();
				res.put("imageUri", returnString);
				res.put("width", bmp.getWidth());
				res.put("height", bmp.getHeight());
				callbackContext.success(res);
			}
		} catch (JSONException e) {
			Log.d("PLUGIN", e.getMessage());
			callbackContext.error(e.getMessage());
		} catch (IOException e) {
			Log.d("PLUGIN", e.getMessage());
			callbackContext.error(e.getMessage());
		} catch (URISyntaxException e) {
			Log.d("PLUGIN", e.getMessage());
			callbackContext.error(e.getMessage());
		}
	}

	private Bitmap getBitmap(String imageUri,BitmapFactory.Options options) throws IOException, URISyntaxException {
		Bitmap bmp;
		URI uri = new URI(imageUri);
		File imageFile = new File(uri);
		bmp = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
		return bmp;
	}

	private Bitmap scaleCropToFit(Bitmap original, int targetWidth, int targetHeight) {
		//Need to scale the image, keeping the aspect ration first
		int width = original.getWidth();
		int height = original.getHeight();

		float widthScale = (float) targetWidth / (float) width;
		float heightScale = (float) targetHeight / (float) height;
		float scaledWidth;
		float scaledHeight;

		int startY = 0;
		int startX = 0;

		if (widthScale > heightScale) {
			scaledWidth = targetWidth;
			scaledHeight = height * widthScale;
			//crop height by...
			startY = (int) ((scaledHeight - targetHeight) / 2);
		} else {
			scaledHeight = targetHeight;
			scaledWidth = width * heightScale;
			//crop width by..
			startX = (int) ((scaledWidth - targetWidth) / 2);
		}

		Bitmap scaledBitmap = Bitmap.createScaledBitmap(original, (int) scaledWidth, (int) scaledHeight, true);
		Bitmap resizedBitmap = Bitmap.createBitmap(scaledBitmap, startX, startY, targetWidth, targetHeight);
		return resizedBitmap;
	}
	
	 protected void storeImage(JSONObject params, String format, Bitmap bmp, CallbackContext callbackContext) throws JSONException, IOException, URISyntaxException {
            int quality = params.getInt("quality");
            String filename = params.getString("filename");
            URI folderUri = new URI(params.getString("directory"));
            URI pictureUri = new URI(params.getString("directory") + "/" + filename);
            File folder = new File(folderUri);
            folder.mkdirs();
            File file = new File(pictureUri);
            OutputStream outStream = new FileOutputStream(file);
            if (format.equals(FORMAT_PNG)) {
                bmp.compress(Bitmap.CompressFormat.PNG, quality,
                        outStream);
            } else {
                bmp.compress(Bitmap.CompressFormat.JPEG, quality,
                        outStream);
            }
            outStream.flush();
            outStream.close();
            JSONObject res = new JSONObject();
            res.put("filename", filename);
            res.put("width", bmp.getWidth());
            res.put("height", bmp.getHeight());
            callbackContext.success(res);
        }
}