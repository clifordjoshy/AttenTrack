package com.leap.attentrack;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

public class AlarmBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        showNotification(context);
    }

    void showNotification(Context context) {
        String CHANNEL_ID = "attentrack";// The id of the channel.
        NotificationCompat.Builder mBuilder;

        String notif_title, notif_text;

        String pending = getPendingAssignments();
        if (pending == null) {
            notif_title = context.getString(R.string.main_notif_title);
            notif_text = context.getString(R.string.main_notif_content);
        } else {
            notif_title = pending;
            notif_text = context.getString(R.string.due_tomorrow_text);
        }

        mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(notif_title)
                .setContentText(notif_text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            CharSequence name = context.getResources().getString(R.string.app_name);// The user-visible name of the channel.
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(mChannel);
        }

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        mBuilder.setContentIntent(contentIntent);
        notificationManager.notify(1, mBuilder.build());
    }

    String getPendingAssignments() {
        LinkedList<AssignmentsFragment.Assignment> assignments_list = null;
        Date tomorrow;
        try {
            FileInputStream fis = new FileInputStream(MainActivity.assignments_file);
            ObjectInputStream inputStream = new ObjectInputStream(fis);
            assignments_list = (LinkedList<AssignmentsFragment.Assignment>) inputStream.readObject();
            inputStream.close();
            fis.close();
            Calendar now = Calendar.getInstance();
            now.add(Calendar.DATE, 1);  //tomorrow
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);
            tomorrow = now.getTime();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
        if (assignments_list != null) {
            for (AssignmentsFragment.Assignment a : assignments_list) {
                if (a.due_date.equals(tomorrow)) {
                    return a.title;
                }
            }
        }

        return null;
    }
}
