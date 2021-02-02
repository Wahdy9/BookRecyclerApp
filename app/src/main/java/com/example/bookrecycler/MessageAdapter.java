package com.example.bookrecycler;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder>{

    //constants to determine which layout to inflate for send and receiver
    public static  final int MSG_TYPE_LEFT = 0;
    public static  final int MSG_TYPE_RIGHT = 1;

    //list to populate
    private List<MessageModel> msgList;
    private Context mContext;


    FirebaseUser fuser;

    public MessageAdapter(Context mContext, List<MessageModel> msgList){
        this.msgList = msgList;
        this.mContext = mContext;
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
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {

        final MessageModel msg = msgList.get(position);

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
                        Toast.makeText(mContext, "Execption: " + e.getMessage(), Toast.LENGTH_LONG).show();
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


    }

    @Override
    public int getItemCount() {
        return msgList.size();
    }


    public  class ViewHolder extends RecyclerView.ViewHolder {

        public TextView show_message;
        public ImageView myLocationIV;

        public ViewHolder(View itemView) {
            super(itemView);
            show_message = itemView.findViewById(R.id.show_message);
            myLocationIV = itemView.findViewById(R.id.myLocation);

        }
    }
}