package hu.dpal.phonegap.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

import java.util.UUID;

import static android.content.Context.MODE_PRIVATE;

public class UniqueDeviceID extends CordovaPlugin {

    public static final String TAG = "UniqueDeviceID";
    public CallbackContext callbackContext;

    protected final static String permission = Manifest.permission.READ_PHONE_STATE;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        try {
            if (action.equals("get")) {
                getDeviceId();
            }else {
                this.callbackContext.error("Invalid action");
                return false;
            }
        } catch(Exception e) {
            this.callbackContext.error("Exception occurred: ".concat(e.getMessage()));
            return false;
        }
        return true;

    }

    protected void getDeviceId(){
        boolean isAndroid10OrGreater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;

        try {
            Context context = cordova.getActivity().getApplicationContext();
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            String uuid;

            if (isAndroid10OrGreater) {
                SharedPreferences preferences = context.getSharedPreferences("UniqueDeviceID", MODE_PRIVATE);

                if (!preferences.contains("device-uuid")) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("device-uuid", UUID.randomUUID().toString());
                    editor.apply();
                }

                uuid = preferences.getString("device-uuid", UUID.randomUUID().toString());
            } else {
                String androidID = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);

                if ("9774d56d682e549c".equals(androidID) || androidID == null) {
                    androidID = "";
                }

                uuid = androidID;
            }

            this.callbackContext.success(uuid);
        }catch(Exception e ) {
            this.callbackContext.error("Exception occurred: ".concat(e.getMessage()));
        }
    }
}
