package hu.dpal.phonegap.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Method;
import java.security.KeyStore;
import java.util.UUID;

import static android.content.Context.MODE_PRIVATE;

public class UniqueDeviceID extends CordovaPlugin {

    public static final String TAG = "UniqueDeviceID";
    public CallbackContext callbackContext;
    public static final int REQUEST_READ_PHONE_STATE = 0;

    protected final static String permission = Manifest.permission.READ_PHONE_STATE;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        try {
            if (action.equals("get")) {
                if(this.hasPermission(permission)){
                    getDeviceId();
                } else {
                    Context context = cordova.getActivity().getApplicationContext();
                    SharedPreferences preferences = context.getSharedPreferences("UniqueDeviceID", MODE_PRIVATE);

                    if (!preferences.contains("read-phone")) {
                        this.requestPermission(this, REQUEST_READ_PHONE_STATE, permission);
                    } else {
                        getDeviceId();
                    }
                }
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

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        for(int r:grantResults)
        {
            if(r == PackageManager.PERMISSION_DENIED)
            {
                //-- This permissions was denied, let remember this and not keep asking
                Context context = cordova.getActivity().getApplicationContext();
                SharedPreferences preferences = context.getSharedPreferences("UniqueDeviceID", MODE_PRIVATE);

                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("read-phone", false);
                editor.apply();

                getDeviceId();
                return;
            }
        }

        if (requestCode == REQUEST_READ_PHONE_STATE) {
            getDeviceId();
        }
    }

    @SuppressLint("MissingPermission")
    protected void getDeviceId(){
        boolean isAndroid10OrGreater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;

        Log.i(TAG, "UUID - " + (isAndroid10OrGreater ? "Is Android 10+" : "Is Android 9-" ));

        try {
            Context context = cordova.getActivity().getApplicationContext();
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            String uuid;
            String androidID = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
            String deviceID;
            String simID;

            if ("9774d56d682e549c".equals(androidID) || androidID == null) {
                androidID = "";
            }

            Log.i(TAG, "UUID - androidID " + androidID);

            if (isAndroid10OrGreater || !this.hasPermission(permission)) {
                SharedPreferences preferences = context.getSharedPreferences("UniqueDeviceID", MODE_PRIVATE);

                if (!preferences.contains("device-uuid")) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("device-uuid", UUID.randomUUID().toString());
                    editor.apply();
                }

                uuid = preferences.getString("device-uuid", UUID.randomUUID().toString());
            } else {
                deviceID = tm.getDeviceId();
                simID = tm.getSimSerialNumber();
                if (deviceID == null) {
                    deviceID = "";
                }
                if (simID == null) {
                    simID = "";
                }
                uuid = androidID + deviceID + simID;
                uuid = String.format("%32s", uuid).replace(' ', '0');
                uuid = uuid.substring(0, 32);
                uuid = uuid.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
            }

            Log.i(TAG, "UUID - " + uuid);

            this.callbackContext.success(uuid);
        }catch(Exception e ) {
            this.callbackContext.error("Exception occurred: ".concat(e.getMessage()));
        }
    }

    private boolean hasPermission(String permission) throws Exception{
        boolean hasPermission = true;
        Method method = null;
        try {
            method = cordova.getClass().getMethod("hasPermission", permission.getClass());
            Boolean bool = (Boolean) method.invoke(cordova, permission);
            hasPermission = bool.booleanValue();
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "Cordova v" + CordovaWebView.CORDOVA_VERSION + " does not support API 23 runtime permissions so defaulting to GRANTED for " + permission);
        }
        return hasPermission;
    }

    private void requestPermission(CordovaPlugin plugin, int requestCode, String permission) throws Exception{
        try {
            java.lang.reflect.Method method = cordova.getClass().getMethod("requestPermission", org.apache.cordova.CordovaPlugin.class ,int.class, java.lang.String.class);
            method.invoke(cordova, plugin, requestCode, permission);
        } catch (NoSuchMethodException e) {
            throw new Exception("requestPermission() method not found in CordovaInterface implementation of Cordova v" + CordovaWebView.CORDOVA_VERSION);
        }
    }
}
