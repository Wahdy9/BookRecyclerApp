package com.example.bookrecycler.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookrecycler.MessageActivity;
import com.example.bookrecycler.R;
import com.example.bookrecycler.models.MessageModel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder>{

    //constants to determine which layout to inflate for send and receiver
    public static  final int MSG_TYPE_LEFT = 0;
    public static  final int MSG_TYPE_RIGHT = 1;

    //list to populate
    private List<MessageModel> msgList;
    private Context mContext;


    FirebaseUser fuser;
    FirebaseFirestore firestore;

    public MessageAdapter(Context mContext, List<MessageModel> msgList){
        this.msgList = msgList;
        this.mContext = mContext;
        firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public int getItemViewType(int position) {
        //check if user is sender or receiver, return the constant for that layout
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        if (msgList.get(position).getSender().equals(fuser.getUid())){
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }


    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate the correct layout for send or receiver based on the constant values
        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_msg_right, parent, false);
            return new MessageAdapter.ViewHolder(view);
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_msg_left, parent, false);
            return new MessageAdapter.ViewHolder(view);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, final int position) {

        //get the msg
        final MessageModel msg = msgList.get(position);

        //format and set time
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            long time = msg.getTimestamp().getTime();

            long now = System.currentTimeMillis();
            CharSequence ago = DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS);
            holder.timeTV.setText(ago);
        }catch(Exception e){
            Log.d("Message Adapter", "onBindViewHolder: " + e.getMessage());
        }

        //check if msg is a map, image or normal text
        if(msg.isMap()) {
            //if the msg is a location
            //set and show image and text "My Location"
            holder.show_message.setText("My Location");
            holder.myLocationIV.setVisibility(View.VISIBLE);

            //when clicked on the image, send user to googleMap
            holder.myLocationIV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Uri gmmIntentUri = Uri.parse("geo:" + msg.getGeoPoint().getLatitude() + "," + msg.getGeoPoint().getLongitude() + "?q=" + msg.getGeoPoint().getLatitude() + "," + msg.getGeoPoint().getLongitude());//with marker
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (mapIntent.resolveActivity(mContext.getPackageManager()) != null) {
                            mContext.startActivity(mapIntent);
                        } else {
                            Toast.makeText(mContext, "Can't open\nyou don't have google map installed", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(mContext, "Something went wrong\n  please try again later", Toast.LENGTH_LONG).show();
                        Log.d("MessageAdapter", "onClick(myLocationIV): " + e.getMessage());
                    }
                }
            });
        }else if(msg.isImage()){
            //if msg is an image

        }else{
            //if msg is just a text
            holder.show_message.setText(msg.getMessage());
            holder.myLocationIV.setVisibility(View.GONE);
        }

        //show alert dialog for deleting msg when click on it
        if(fuser.getUid().equals(msg.getSender())) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //show alert dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle("Delete");
                    builder.setMessage("Are you sure you want to delete this message?");
                    builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteMessage(msg.getId());
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                }
            });
        }

    }

    //Delete the msg
    private void deleteMessage( final String msgId) {
        //create updated msg
        HashMap<String, Object> updatedMsgMap = new HashMap<>();
        updatedMsgMap.put("map", false);
        updatedMsgMap.put("image", false);
        updatedMsgMap.put("message", "This message was deleted");

        //update the document in firestore
        firestore.collection("Chats").document(msgId).update(updatedMsgMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                //refresh MessageActivity by removing and reAdding the snapshot listener
                if (mContext instanceof MessageActivity) {
                    ((MessageActivity)mContext).registration.remove();
                    ((MessageActivity)mContext).readMessages();
                }

            }
        });
    }

    @Override
    public int getItemCount() {
        return msgList.size();
    }


    public  class ViewHolder extends RecyclerView.ViewHolder {

        public TextView show_message, timeTV;
        public ImageView myLocationIV;

        public ViewHolder(View itemView) {
            super(itemView);
            show_message = itemView.findViewById(R.id.show_message);
            myLocationIV = itemView.findViewById(R.id.myLocation);
            timeTV = itemView.findViewById(R.id.msgTimeTV);

        }
    }
}