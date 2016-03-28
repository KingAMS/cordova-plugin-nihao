package com.nihaolabs.nihao.cordova;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import java.util.concurrent.atomic.AtomicInteger;


public class NH extends CordovaPlugin implements OnInitListener {

    public static final String ERR_INVALID_OPTIONS = "ERR_INVALID_OPTIONS";
    public static final String ERR_NOT_INITIALIZED = "ERR_NOT_INITIALIZED";
    public static final String ERR_ERROR_INITIALIZING = "ERR_ERROR_INITIALIZING";
    public static final String ERR_UNKNOWN = "ERR_UNKNOWN";

    boolean ttsInitialized = false;
    TextToSpeech tts = null;
    private AtomicInteger incrementer;

    @Override
    public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
        tts = new TextToSpeech(cordova.getActivity().getApplicationContext(), this);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                // do nothing
            }

            @Override
            public void onDone(String callbackId) {
                if (!callbackId.equals("")) {
                    int lav = incrementer.decrementAndGet();
                    log("lav: "+lav);
                    if(lav == 0){
                        log("in");
                        CallbackContext context = new CallbackContext(callbackId, webView);
                        context.success("Done Loading");
                    }
                }
            }

            @Override
            public void onError(String callbackId) {
                if (!callbackId.equals("")) {
                    CallbackContext context = new CallbackContext(callbackId, webView);
                    context.error(ERR_UNKNOWN);
                }
            }
        });
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext)
            throws JSONException {
        if (action.equals("checktts")) {
            checkTTS(args, callbackContext);
        }
        else {
            return false;
        }
        return true;
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            tts = null;
        } else {
            // warm up the tts engine with an empty string
            HashMap<String, String> ttsParams = new HashMap<String, String>();
            ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");
            tts.setLanguage(new Locale("en", "US"));
            tts.speak("", TextToSpeech.QUEUE_FLUSH, ttsParams);

            ttsInitialized = true;
        }
    }

    private void log(String text){
         final String str = text;

         webView.getView().post(new Runnable() {
                            @Override
                            public void run() {
                                webView.loadUrl("javascript:console.log('"+str+"');");
                            }
                        });
    }

    private void checkTTS(JSONArray args, CallbackContext callbackContext)
                    throws JSONException, NullPointerException {

               log("TTS Engine and Languages test started");



                if (args == null) {
                    callbackContext.error(ERR_INVALID_OPTIONS);
                    return;
                }

                if (tts == null) {
                    callbackContext.error(ERR_ERROR_INITIALIZING);
                    return;
                }

                if (!ttsInitialized) {
                    callbackContext.error(ERR_NOT_INITIALIZED);
                    return;
                }

                 HashMap<String, String> ttsParams = new HashMap<String, String>();
                 ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, callbackContext.getCallbackId());
                 ttsParams.put(TextToSpeech.Engine.KEY_PARAM_VOLUME,"0");
                 tts.setSpeechRate((float) 6);

                incrementer = new AtomicInteger (args.length());
                boolean needUpdate = false;
                for (int i = 0 ; i < args.length(); i++) {
                       String locale = args.getString(i);
                        log("Checking: "+locale);
                        String[] localeArgs = locale.split("-");
                        Locale tempLocale = new Locale(localeArgs[0], localeArgs[1]);
                        Set<String> myset = tts.getFeatures(tempLocale);
                        if(myset == null){
                            log("getFeatures returned null for "+locale);
							tts.setLanguage(tempLocale);
                            tts.speak(locale, TextToSpeech.QUEUE_ADD, ttsParams);
                        }
                        else{
                            String[] res = myset.toArray(new String[myset.size()]);
                            JSONArray jsonArr = new JSONArray(res);
                            String myJson  = jsonArr.toString();
                            log(locale +" Features:" +myJson);
                            if(!myset.contains("networkTts")){
                                 log(locale +" Not Supported");
                                 incrementer.decrementAndGet();
                                  //callbackContext.error(locale +" Not Supported");
                            }
                            else{
                                if(!myset.contains("embeddedTts")){
                                    needUpdate = true;
                                    log(locale +" Not Installed locally");
                                    tts.setLanguage(tempLocale);
                                    tts.speak(locale, TextToSpeech.QUEUE_ADD, ttsParams);
                                }
                                else{
                                    incrementer.decrementAndGet();
                                }
                            }
                        }
                    }

                ;
                if(!needUpdate)
    			    callbackContext.success("res:OK");
            }

}