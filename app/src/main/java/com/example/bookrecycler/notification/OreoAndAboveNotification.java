package com.example.bookrecycler.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

//Oreo+ OS require to creat a channel to send or receive notification, this class handle it
public class OreoAndAboveNotification extends ContextWrapper{

    public static final String ID = "some_id";//channel id
    public static final String NAME = "Chat notifications";//channel name

    private NotificationManager notificationManager;

    public OreoAndAboveNotification(Context base){
        super(base);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createChannel();
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationChannel notificationChannel = new NotificationChannel(ID,NAME, NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.enableLights(true);
        //notificationChannel.enableVibration(true);//for vibration
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        getManager().createNotificationChannel(notificationChannel);

    }
    public NotificationManager getManager(){
        if(notificationManager == null){
            notificationManager =(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }

    //return oreo notification
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getONotification(String title,
                                                 String body,
                                                 PendingIntent pIntent,
                                                 Uri soundUri,
                                                 String icon){

        return new Notification.Builder(getApplicationContext(), ID)
                .setContentIntent(pIntent)
                .setContentTitle(title)
                .setContentText(body)
                .setSound(soundUri)
                .setAutoCancel(true)
                .setSmallIcon(Integer.parseInt(icon));

    }

}
