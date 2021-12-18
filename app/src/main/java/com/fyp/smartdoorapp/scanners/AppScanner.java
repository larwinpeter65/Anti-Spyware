package com.fyp.smartdoorapp.scanners;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import com.fyp.smartdoorapp.R;
import com.fyp.smartdoorapp.activities.AppDetails;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AppScanner extends AsyncTask<Void, String, Void> {
    private WeakReference<Context> contextRef;

    private Interpreter tflite = null;
    private JSONArray p_jArray = null;
    private JSONArray i_jArray = null;

    private String packageName;
    private String appName;
    private String prediction;
    private boolean skipScan = false;

    private float predictionScore;
    private boolean withSysApps;

    private ArrayList<String> appPermissionsList= new ArrayList<>();

    public AppScanner(Context context, String pkgName) {
        contextRef = new WeakReference<>(context);
        this.packageName = pkgName;
        this.withSysApps = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean("includeSystemApps", false);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        float[] inputVal = new float[2000];

        try {
            tflite = new Interpreter(loadModelFile(), null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            // Loading the features.json from assets folder.
            JSONObject obj = new JSONObject(loadJSONFromAsset());
            p_jArray = obj.getJSONArray("permissions");// This array stores permissions from features.json file
            i_jArray = obj.getJSONArray("intents");// This array  stores intents from features.json file
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final PackageManager pm = contextRef.get().getPackageManager();

        try {
            if ((pm.getPackageInfo(packageName, 0).applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1 && !withSysApps){
                skipScan = true;
                return null;
            } else {
                boolean flag = false;// flag is true if and only if the app under scan contains at least one permission or intent-filter defined in features.json
                appName = pm.getPackageInfo(packageName, 0).applicationInfo.loadLabel(pm).toString();
                appPermissionsList = getListOfPermissions(contextRef.get().createPackageContext(packageName, 0));
                ArrayList<String> appIntentsList = getListOfIntents(contextRef.get().createPackageContext(packageName, 0));

                String str;

                if (appPermissionsList.size() == 0 && appIntentsList.size() == 0) {
                    prediction = contextRef.get().getString(R.string.unknown);
                    return null;
                }

                for (int i = 0; i < p_jArray.length(); i++) {
                    str = p_jArray.optString(i);
                    if ((appPermissionsList.contains(str))) {
                        inputVal[i] = 1;
                        flag = true;
                    } else {
                        inputVal[i] = 0;
                    }
                }


                for (int i = 0; i < i_jArray.length(); i++) {
                    str = i_jArray.optString(i);
                    if ((appIntentsList.contains(str))) {
                        inputVal[i + 489] = 1;
                        flag = true;
                    } else {
                        inputVal[i + 489] = 0;
                    }
                }

                if (!flag) {
                    prediction = contextRef.get().getString(R.string.unknown);
                    return null;
                }

                float[][] outputVal = new float[1][1];

                // Run the model
                tflite.run(inputVal, outputVal);
                float inferredValue = outputVal[0][0];
                predictionScore = inferredValue;
                if (inferredValue > 0.75) {
                    prediction = contextRef.get().getString(R.string.spyware);
                } else if (inferredValue > 0.5) {
                    prediction = contextRef.get().getString(R.string.risky);
                } else {
                    prediction = contextRef.get().getString(R.string.safe);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if(!skipScan) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(contextRef.get());
            String CHANNEL_ID = "channel_100";
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(contextRef.get(), CHANNEL_ID);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String CHANNEL_NAME = "SPYWARE SCANNER";
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(mChannel);
            }
            int NOTIFICATION_ID = 100;
            Intent intent = new Intent(contextRef.get(), AppDetails.class);
            intent.putExtra("appName", appName);
            intent.putExtra("packageName", packageName);
            intent.putExtra("result", prediction);
            intent.putExtra("prediction", predictionScore);
            intent.putExtra("scan_mode", "realtime_scan");
            intent.putStringArrayListExtra("permissionList", appPermissionsList);
            PendingIntent contentIntent = PendingIntent.getActivity(contextRef.get(), 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            if (prediction.equalsIgnoreCase(contextRef.get().getString(R.string.safe))) {
                notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                        .setAutoCancel(true)
                        .setContentTitle("Spyware Scanner")
                        .setContentIntent(contentIntent)
                        .setContentText(appName + contextRef.get().getString(R.string.is) + prediction)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);
                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
            } else {
                NOTIFICATION_ID = (int) (new Date()).getTime();
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE);
                uninstallIntent.setData(Uri.parse("package:" + packageName));
                PendingIntent uninstallPendingIntent = PendingIntent.getActivity(contextRef.get(), NOTIFICATION_ID + 1, uninstallIntent, PendingIntent.FLAG_ONE_SHOT);
                PendingIntent contentIntent1 = PendingIntent.getActivity(contextRef.get(), NOTIFICATION_ID + 2, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                notificationBuilder.setSmallIcon(R.drawable.ic_notification)
                        .setAutoCancel(true)
                        .setVibrate(new long[]{250, 250, 250, 250})
                        .setContentTitle("Spyware Scanner")
                        .setContentText(appName + contextRef.get().getString(R.string.is) + prediction)
                        .setContentIntent(contentIntent1)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .addAction(R.drawable.ic_delete_notification, contextRef.get().getString(R.string.uninstall).toUpperCase(), uninstallPendingIntent);
                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
            }
        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = contextRef.get().getAssets().openFd("saved_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Load the JSON file from assets folder
     *
     * @return String containing contents of JSON file
     * <p>
     * Borrowed from: https://stackoverflow.com/questions/19945411 (GrIsHu)
     */
    private String loadJSONFromAsset() {
        String json;
        try {
            InputStream is = contextRef.get().getAssets().open("features.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    /**
     * Get the list of permissions used by the application
     * <p>
     * Borrowed from: https://stackoverflow.com/questions/18236801 (Yousha Aleayoub)
     */
    private static ArrayList<String> getListOfPermissions(final Context context) {
        ArrayList<String> arr = new ArrayList<>();

        try {
            final AssetManager am = context.createPackageContext(context.getPackageName(), 0).getAssets();
            final Method addAssetPath = am.getClass().getMethod("addAssetPath", String.class);
            final int cookie = (Integer) addAssetPath.invoke(am, context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).sourceDir);
            final XmlResourceParser xmlParser = am.openXmlResourceParser(cookie, "AndroidManifest.xml");
            int eventType = xmlParser.next();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if ((eventType == XmlPullParser.START_TAG) && "uses-permission".equals(xmlParser.getName())) {
                    for (byte i = 0; i < xmlParser.getAttributeCount(); i++) {
                        if (xmlParser.getAttributeName(i).equals("name")) {
                            arr.add(xmlParser.getAttributeValue(i));
                        }
                    }
                }
                eventType = xmlParser.next();
            }
            xmlParser.close();
        } catch (final XmlPullParserException exception) {
            exception.printStackTrace();
        } catch (final PackageManager.NameNotFoundException exception) {
            exception.printStackTrace();
        } catch (final IOException exception) {
            exception.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return arr;

    }

    private static ArrayList<String> getListOfIntents(final Context context) {
        ArrayList<String> arr = new ArrayList<>();

        try {
            final AssetManager am = context.createPackageContext(context.getPackageName(), 0).getAssets();
            final Method addAssetPath = am.getClass().getMethod("addAssetPath", String.class);
            final int cookie = (Integer) addAssetPath.invoke(am, context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).sourceDir);
            final XmlResourceParser xmlParser = am.openXmlResourceParser(cookie, "AndroidManifest.xml");
            int eventType = xmlParser.next();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if ((eventType == XmlPullParser.START_TAG) && "action".equals(xmlParser.getName())) {
                    for (byte i = 0; i < xmlParser.getAttributeCount(); i++) {
                        if (xmlParser.getAttributeName(i).equals("name")) {
                            arr.add(xmlParser.getAttributeValue(i));
                        }
                    }
                }
                eventType = xmlParser.next();
            }
            xmlParser.close();
        } catch (final XmlPullParserException exception) {
            exception.printStackTrace();
        } catch (final PackageManager.NameNotFoundException exception) {
            exception.printStackTrace();
        } catch (final IOException exception) {
            exception.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return arr;
    }
}
