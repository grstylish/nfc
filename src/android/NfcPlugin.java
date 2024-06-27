package cordova.isimplelab.nfc.plugin;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.util.Log;

import reader.src.main.java.com.pro100svitlo.creditCardNfcReader.CardNfcAsyncTask;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class NfcPlugin extends CordovaPlugin {

    private static final String TAG = "MyCordovaPlugin";

    public static final String CARD_NUMBER = "number";
    public static final String CARD_DATE = "date";
    public static final String CARD_TYPE = "type";

    private static final String STATUS_NFC_OK = "NFC_OK";
    private static final String STATUS_NO_NFC = "NO_NFC";
    private static final String STATUS_NFC_DISABLED = "NFC_DISABLED";
    private static final String ACTION_NFC_SETTINGS = "ACTION_NFC_SETTINGS";

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    private CardNfcAsyncTask cardNfcAsyncTask;

    private CallbackContext callbackContext;
    private Activity activity;

    public NfcPlugin() {}

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(activity, pendingIntent, null, null);
        }
    }

    @Override
    public void onPause(boolean multitasking) {
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(activity);
        }
        super.onPause(multitasking);
    }

    @Override
    protected void pluginInitialize() {
        this.cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                activity = cordova.getActivity();
                nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
                int flag;
                if (Build.VERSION.SDK_INT >= 31) flag = android.app.PendingIntent.FLAG_IMMUTABLE;
                else flag = 0;
                pendingIntent = PendingIntent.getActivity(activity, 0, new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), flag);
            }
        });
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        this.cordova.getThreadPool().execute(new Runnable() {
            public void run() {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                if (tag != null) {
                    cardNfcAsyncTask = new CardNfcAsyncTask.Builder(new CardNfcAsyncTask.CardNfcInterface() {
                        @Override
                        public void startNfcReadCard() {}

                        @Override
                        public void cardIsReadyToRead() {
                            String cardNumber = cardNfcAsyncTask.getCardNumber();
                            String expiredDate = cardNfcAsyncTask.getCardExpireDate();
                            String cardType = cardNfcAsyncTask.getCardType();

                            JSONObject json = new JSONObject();
                            try {
                                json.put(CARD_NUMBER, cardNumber);
                                json.put(CARD_DATE, expiredDate);
                                json.put(CARD_TYPE, cardType);
                            } catch (JSONException ignored) {}

                            PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, json);
                            pluginresult.setKeepCallback(true);
                            callbackContext.sendPluginResult(pluginresult);

                        }

                        @Override
                        public void doNotMoveCardSoFast() {}

                        @Override
                        public void unknownEmvCard() {
                            JSONObject json = new JSONObject();
                            try {
                                json.put("error", "unknownEmvCard");
                            }  catch (JSONException ignored) {}

                            PluginResult pluginresult = new PluginResult(PluginResult.Status.ERROR, json);
                            pluginresult.setKeepCallback(true);
                            callbackContext.sendPluginResult(pluginresult);
                        }

                        @Override
                        public void cardWithLockedNfc() {
                            JSONObject json = new JSONObject();
                            try {
                                json.put("error", "cardWithLockedNfc");
                            }  catch (JSONException ignored) {}

                            PluginResult pluginresult = new PluginResult(PluginResult.Status.ERROR, json);
                            pluginresult.setKeepCallback(true);
                            callbackContext.sendPluginResult(pluginresult);
                        }

                        @Override
                        public void finishNfcReadCard() {
                        }
                    }, intent, false).build();

                }
            }
        });


    }

    private void showSettings(CallbackContext callbackContext) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_NFC_SETTINGS);
            cordova.getActivity().startActivity(intent);
            callbackContext.success(ACTION_NFC_SETTINGS);
        } catch (Exception ignored) {
            callbackContext.error(ACTION_NFC_SETTINGS);
        }

    }

    private String getNfcStatus() {
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(cordova.getActivity());
            if (nfcAdapter == null) {
                return STATUS_NO_NFC;
            } else if (!nfcAdapter.isEnabled()) {
                return STATUS_NFC_DISABLED;
            } else {
                return STATUS_NFC_OK;
            }
        }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("scan")) {
            this.callbackContext = callbackContext;
            return true;
        } else if (action.equals("showSettings")) {
            showSettings(callbackContext);
            return true;
        } else if (action.equals("enabled")) {
           if (!getNfcStatus().equals(STATUS_NFC_OK)) {
               callbackContext.error(getNfcStatus());
               return true;
           } else {
               callbackContext.success(getNfcStatus());
               return true;
           }
        }

        return false;
    }
}
