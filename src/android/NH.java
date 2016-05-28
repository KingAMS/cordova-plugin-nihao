package com.nihaolabs.nihao.cordova;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import java.util.concurrent.atomic.AtomicInteger;


public class NH extends CordovaPlugin  {
	TTS nhtts = null;
	
	
	@Override
	public void initialize(CordovaInterface cordova, final CordovaWebView webView){
		this.nhtts = new TTS();
		this.nhtts.initialize(cordova, webView);
	}
	
	
	@Override
	public boolean execute(String action, JSONArray params, CallbackContext callbackContext)
	throws JSONException {
		if (action.equals("checktts")) {
			nhtts.run(params, callbackContext);
		}
		else  if (action.equals("cropAndResize")) {
			CropResizeImage resizeImage = new CropResizeImage(params.getJSONObject(0), callbackContext);
			cordova.getThreadPool().execute(resizeImage);
		}
		else {
			return false;
		}
		return true;
	}

}
