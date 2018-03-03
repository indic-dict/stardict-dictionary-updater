package sanskritcode.sanskritdictionaryupdater;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

abstract class BaseActivity extends Activity {
    protected DictIndexStore dictIndexStore = null;

    void getPermission(String permissionString, Activity activity) {
        final String LOGGER_TAG = getLocalClassName();
        if (ContextCompat.checkSelfPermission(activity, permissionString) == PackageManager.PERMISSION_GRANTED) {
            Log.d(LOGGER_TAG, "getPermission: Got permission: " + permissionString);
        } else {
            Log.w(LOGGER_TAG, "getPermission: Don't have permission: " + permissionString);
            ActivityCompat.requestPermissions(activity, new String[]{permissionString}, 101);
            Log.i(LOGGER_TAG, "getPermission: new permission: " + ContextCompat.checkSelfPermission(activity, permissionString));
        }
    }

    protected String getVersion() {
        final String LOGGER_TAG = getLocalClassName();
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(LOGGER_TAG, "getVersion:" + "Version getting failed.");
            e.printStackTrace();
            return "Version getting failed.";
        }
    }


    public static void largeLog(String tag, String content) {
        if (content.length() > 4000) {
            Log.v(tag, tag + ":" + content.substring(0, 4000));
            largeLog(tag, content.substring(4000));
        } else {
            Log.v(tag, tag + ":" + content);
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("dictIndexStore", dictIndexStore);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey("dictIndexStore")) {
            dictIndexStore = (DictIndexStore) savedInstanceState.get("dictIndexStore");
        }
    }

    public void sendLoagcatMail(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED) {
            Log.d(this.getLocalClassName(), "SendLoagcatMail: " + "Got READ_LOGS permissions");
        } else {
            Log.e(this.getLocalClassName(), "SendLoagcatMail: " + "Don't have READ_LOGS permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_LOGS}, 103);
            Log.i(this.getLocalClassName(), "SendLoagcatMail: " + "new READ_LOGS permission: " + ContextCompat.checkSelfPermission(this, Manifest.permission.READ_LOGS));
        }
        // save logcat in file
        File outputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),
                "logcat.txt");
        Log.i(this.getLocalClassName(), "SendLoagcatMail: " + "logcat file is " + outputFile.getAbsolutePath());


        String deviceDetails= "Device details:";
        deviceDetails += "\n OS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")";
        deviceDetails += "\n OS API Level: "+android.os.Build.VERSION.RELEASE + "("+android.os.Build.VERSION.SDK_INT+")";
        deviceDetails += "\n Device: " + android.os.Build.DEVICE;
        deviceDetails += "\n Model (and Product): " + android.os.Build.MODEL + " ("+ android.os.Build.PRODUCT + ")";
        Log.i(this.getLocalClassName(), "SendLoagcatMail: " + "deviceDetails: " + deviceDetails);

        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;
        Log.i(this.getLocalClassName(), "SendLoagcatMail: " + "App version: " + versionName + " with id " + versionCode);

        try {
            Runtime.getRuntime().exec(
                    "logcat -f " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(this.getLocalClassName(), "SendLoagcatMail: " + "Alas error! ", e);
        }

        //send file using email
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        // Set type to "email"
        emailIntent.setType("vnd.android.cursor.dir/email");
        String to[] = {"vishvas.vasuki+STARDICTAPP@gmail.com"};
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
        // the attachment
        emailIntent .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(outputFile));
        // the mail subject
        emailIntent .putExtra(Intent.EXTRA_SUBJECT, "Stardict Updater App Failure report.");
        this.startActivity(Intent.createChooser(emailIntent , "Email failure report to maker?..."));
    }

    void showNetworkInfo(TextView warningText) {
        ConnectivityManager cm =
                (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = null;
        if (cm != null) {
            activeNetwork = cm.getActiveNetworkInfo();
        }
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        boolean isWiFi = activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;

        warningText.setBackgroundColor(Color.LTGRAY);
        warningText.setTextColor(Color.RED);
        if (isConnected) {
            if (isWiFi) {
                warningText.setVisibility(View.INVISIBLE);
            } else {
                warningText.setText(R.string.connected_nowifi);
                warningText.setVisibility(View.VISIBLE);
            }
        } else {
            warningText.setText(R.string.noConnection);
            warningText.setVisibility(View.VISIBLE);
        }

    }

}
