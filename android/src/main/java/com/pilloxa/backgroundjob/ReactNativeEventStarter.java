package com.pilloxa.backgroundjob;

import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import javax.annotation.Nullable;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class ReactNativeEventStarter {
  private static final String LOG_TAG = ReactNativeEventStarter.class.getSimpleName();
  private final ReactNativeHost reactNativeHost;
  private final Context context;
  private static final String CONTEXT_TITLE_SETTING = "CONTEXT_TITLE_SETTING";
  private static final String CONTEXT_TEXT_SETTING = "CONTEXT_TEXT_SETTING";
  private static final String SETTINGS_KEY = "Background_Job_Settings";
 private static final String CHANNEL_ID = "RN_BACKGROUND_ACTIONS_CHANNEL";
    private static final int SERVICE_NOTIFICATION_ID = 92901;

  public ReactNativeEventStarter(@NonNull Context context) {
    this.context = context;
    reactNativeHost = ((ReactApplication) context.getApplicationContext()).getReactNativeHost();
  }

  public void trigger(@NonNull Bundle jobBundle) {
    Log.d(LOG_TAG, "trigger() called with: jobBundle = [" + jobBundle + "]");
    boolean appInForeground = Utils.isReactNativeAppInForeground(reactNativeHost);
    boolean allowExecutionInForeground = jobBundle.getBoolean("allowExecutionInForeground", false);
    SharedPreferences.Editor editor = this.context.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE).edit();
    editor.putString(CONTEXT_TEXT_SETTING,jobBundle.getString("notificationText"));
    editor.putString(CONTEXT_TITLE_SETTING, jobBundle.getString("notificationTitle"));
    editor.apply();
    if (!appInForeground || allowExecutionInForeground) {
      // Will execute if the app is in background, or in forground but it has permision to do so
      MyHeadlessJsTaskService.start(context, jobBundle);
    }
  }

  public static class MyHeadlessJsTaskService extends HeadlessJsTaskService {
    private static final String LOG_TAG = MyHeadlessJsTaskService.class.getSimpleName();

    @Override
    @SuppressLint("WrongConstant")
    public void onCreate() {
      super.onCreate();

        Context mContext = this.getApplicationContext();
//       if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//         NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_LOW);
//         ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
//
// //         Notification notification =
// //                 new Notification.Builder(mContext, CHANNEL_ID)
// //                         .setContentTitle(contextTitle)
// //                         .setContentText(contextText)
// //                         .setSmallIcon(R.drawable.ic_notification)
// //                         .build();
// //
// //         startForeground(1, notification);
//       }

  SharedPreferences preferences = mContext.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE);
        String contextTitle = preferences.getString(CONTEXT_TITLE_SETTING, "Running in background...");
        String contextText = preferences.getString(CONTEXT_TEXT_SETTING, "Background job");
 createNotificationChannel(contextTitle, contextText);
      // Create the notification
              final Intent notificationIntent = new Intent(this, ReactActivity.class);
              final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
              final Notification notification = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                      .setContentTitle(contextTitle)
                      .setContentText(contextText)
                      .setSmallIcon(R.drawable.ic_notification)
                      .setContentIntent(contentIntent)
                      .setOngoing(true)
                      .setPriority(NotificationCompat.PRIORITY_MIN)
//                       .setColor(color)
                      .build();

                      startForeground(1, notification);

    }

    @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            final Bundle extras = intent.getExtras();
            if (extras == null) {
                throw new IllegalArgumentException("Extras cannot be null");
            }
            // Get info
            final String taskTitle = extras.getString(CONTEXT_TITLE_SETTING, "Running in background...");
            final String taskDesc = extras.getString(CONTEXT_TEXT_SETTING, "Background job");

            // Turning into a foreground service
            createNotificationChannel(taskTitle, taskDesc); // Necessary creating channel for API 26+
            // Create the notification
//             final Intent notificationIntent = new Intent(this, ReactActivity.class);
//             final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            final Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(taskTitle)
                    .setContentText(taskDesc)
//                     .setSmallIcon(iconInt)
//                     .setContentIntent(contentIntent)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
//                     .setColor(color)
                    .build();
            startForeground(SERVICE_NOTIFICATION_ID, notification);
            return super.onStartCommand(intent, flags, startId);
        }

          private void createNotificationChannel(@NonNull final String taskTitle, @NonNull final String taskDesc) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    final int importance = NotificationManager.IMPORTANCE_LOW;
                    final NotificationChannel channel = new NotificationChannel(CHANNEL_ID, taskTitle, importance);
                    channel.setDescription(taskDesc);
                    final NotificationManager notificationManager = getSystemService(NotificationManager.class);
                    notificationManager.createNotificationChannel(channel);
                }
            }

    @Nullable @Override protected HeadlessJsTaskConfig getTaskConfig(Intent intent) {
      Log.d(LOG_TAG, "getTaskConfig() called with: intent = [" + intent + "]");
      Bundle extras = intent.getExtras();
      boolean allowExecutionInForeground = extras.getBoolean("allowExecutionInForeground", false);
      long timeout = extras.getLong("timeout", 2000);
      // For task with quick execution period additional check is required
      ReactNativeHost reactNativeHost =
          ((ReactApplication) getApplicationContext()).getReactNativeHost();
      boolean appInForeground = Utils.isReactNativeAppInForeground(reactNativeHost);
      if (appInForeground && !allowExecutionInForeground) {
        return null;
      }
      return new HeadlessJsTaskConfig(intent.getStringExtra("jobKey"), Arguments.fromBundle(extras),
          timeout, allowExecutionInForeground);
    }

    public static void start(Context context, Bundle jobBundle) {
      Log.d(LOG_TAG,
          "start() called with: context = [" + context + "], jobBundle = [" + jobBundle + "]");
      Intent starter = new Intent(context, MyHeadlessJsTaskService.class);
      starter.putExtras(jobBundle);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(starter);
      }else{
        context.startService(starter);
      }
    }
  }
}
