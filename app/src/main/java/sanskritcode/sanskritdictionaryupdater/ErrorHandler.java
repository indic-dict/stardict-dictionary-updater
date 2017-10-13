package sanskritcode.sanskritdictionaryupdater;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.IOException;

class ErrorHandler {
    public static void sendLoagcatMail(Activity baseActivity){
        if (ContextCompat.checkSelfPermission(baseActivity, Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED) {
            Log.d(baseActivity.getLocalClassName(), "Got READ_LOGS permissions");
        } else {
            Log.e(baseActivity.getLocalClassName(), "Don't have READ_LOGS permissions");
            ActivityCompat.requestPermissions(baseActivity, new String[]{Manifest.permission.READ_LOGS}, 103);
            Log.i(baseActivity.getLocalClassName(), "new READ_LOGS permission: " + ContextCompat.checkSelfPermission(baseActivity, Manifest.permission.READ_LOGS));
        }
        // save logcat in file
        File outputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),
                "logcat.txt");
        Log.i("SendLoagcatMail: ", "logcat file is " + outputFile.getAbsolutePath());


        String deviceDetails= "Device details:";
        deviceDetails += "\n OS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")";
        deviceDetails += "\n OS API Level: "+android.os.Build.VERSION.RELEASE + "("+android.os.Build.VERSION.SDK_INT+")";
        deviceDetails += "\n Device: " + android.os.Build.DEVICE;
        deviceDetails += "\n Model (and Product): " + android.os.Build.MODEL + " ("+ android.os.Build.PRODUCT + ")";
        Log.i("SendLoagcatMail: ", "deviceDetails: " + deviceDetails);

        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;
        Log.i("SendLoagcatMail: ", "App version: " + versionName + " with id " + versionCode);

        try {
            Runtime.getRuntime().exec(
                    "logcat -f " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(baseActivity.getLocalClassName(), "Alas error! ", e);
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
        baseActivity.startActivity(Intent.createChooser(emailIntent , "Email failure report to maker?..."));
    }

}
