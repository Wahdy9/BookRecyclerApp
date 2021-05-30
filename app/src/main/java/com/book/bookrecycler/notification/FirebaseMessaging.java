package com.book.bookrecycler.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.book.bookrecycler.MessageActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

//Here we receive the notifications
public class FirebaseMessaging extends FirebaseMessagingService {


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        //get current user id from shared preferences, because the app may not be running, only the service is running
        SharedPreferences sp = getSharedPreferences("SP_USER",MODE_PRIVATE);
        String savedCurrentUser = sp.getString("Current_USERID","None");

        //chat notification
        //get sender and reciever from the msg
        String sent = remoteMessage.getData().get("sent");//receiver
        String user = remoteMessage.getData().get("user");//sender
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if(fUser != null && sent.equals(fUser.getUid())){
            //don't show notification for the sender
            if(!savedCurrentUser.equals(user)){
                //check if +Oreo and show the notification
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    sendOAndAboveNotification(remoteMessage);
                }else{
                    sendNormalNotification(remoteMessage);
                }
            }
        }


    }


    //show notification in my device
    private void sendNormalNotification(RemoteMessage remoteMessage) {
        //get notification data to display it
        String user = remoteMessage.getData().get("user");
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        //generate a random id for notification by removing non digit character
        int i = Integer.parseInt(user.replaceAll("[\\D]", ""));
        //intent to handle when user click on notification...it will open ChatActivity with userId
        Intent intent = new Intent(this, MessageActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("userId",user);
        intent.putExtras(bundle);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pIntent = PendingIntent.getActivity(this,i,intent,PendingIntent.FLAG_ONE_SHOT);

        //sound of notification
        Uri defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        //build the notification
        NotificationCompat.Builder builder =new NotificationCompat.Builder(this)
                .setSmallIcon(Integer.parseInt(icon))
                .setContentText(body)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setSound(defSoundUri)
                .setContentIntent(pIntent);

        //get notification manager
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int j = 0;
        if(i>0){ //??
            j = i;
        }
        //show notification
        notificationManager.notify(j,builder.build());

    }

    //show notification in my device if running android Oreo
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendOAndAboveNotification(RemoteMessage remoteMessage) {
        //get notification data to display it.. sent from ChatActivity
        String user = remoteMessage.getData().get("user");
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        int i = Integer.parseInt(user.replaceAll("[\\D]", ""));
        //intent to handle when user click on notification...it will open MessageActivity with userId
        Intent intent = new Intent(this, MessageActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("userId",user);
        intent.putExtras(bundle);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pIntent = PendingIntent.getActivity(this,i,intent,PendingIntent.FLAG_ONE_SHOT);

        //sound of notification
        Uri defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);


        OreoAndAboveNotification notification1 = new OreoAndAboveNotification(this);
        //build the notification(build in OreoAndAboveNotification)
        Notification.Builder builder  =  notification1.getONotification(title,body,pIntent,defSoundUri,icon);

        int j = 0;//??
        if(i>0){
            j = i;
        }
        //show notification
        notification1.getManager().notify(j,builder.build());
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);

        //update user token
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null){
            //user sign in,update token
            updateToken(s);

        }

    }

    private void updateToken(String tokenRefresh) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Token token = new Token(tokenRefresh);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("Tokens").document(user.getUid()).set(token);
    }

}
