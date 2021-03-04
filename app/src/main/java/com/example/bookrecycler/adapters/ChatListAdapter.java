package com.example.bookrecycler.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bookrecycler.ChatListActivity;
import com.example.bookrecycler.MainActivity;
import com.example.bookrecycler.MessageActivity;
import com.example.bookrecycler.R;
import com.example.bookrecycler.models.UserModel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    //list to populate
    private List<UserModel> userList;
    private Context mContext;

    //firebase
    FirebaseAuth mAuth;
    FirebaseFirestore firestore;

    public ChatListAdapter(Context mContext, List<UserModel> userList){
        this.userList = userList;
        this.mContext = mContext;
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_chatlist, parent, false);
        return new ChatListAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        final UserModel user = userList.get(position);

        //set username
        holder.username.setText(user.getName());

        //set image profile
        if (user.getImg_url().equals("default")){
            holder.profile_image.setImageResource(R.drawable.user_profile);
        } else {
            Glide.with(mContext).load(user.getImg_url()).into(holder.profile_image);
        }

        //query to check if there is new msgs, if so show the badge
        firestore.collection("Chatlist").document(mAuth.getUid()).collection("Contacted").document(user.getId()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(documentSnapshot != null){
                    if(documentSnapshot.getBoolean("newMsgs") != null &&  documentSnapshot.getBoolean("newMsgs")){
                        holder.newMsgsBadge.setVisibility(View.VISIBLE);
                    }else{
                        holder.newMsgsBadge.setVisibility(View.GONE);
                    }
                }
            }
        });

        //when click on item go to message activity
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //hide the badge
                holder.newMsgsBadge.setVisibility(View.GONE);
                //set the new message in firestore to false, so it wont show the badge next time
                Map<String, Object> senderMap = new HashMap<>();
                senderMap.put("newMsgs",false);
                firestore.collection("Chatlist").document(mAuth.getUid()).collection("Contacted").document(user.getId()).update(senderMap);
                //start MessageActivity
                Intent intent = new Intent(mContext, MessageActivity.class);
                intent.putExtra("userId", user.getId());
                mContext.startActivity(intent);
            }
        });

        //delete chatlist on long press
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Show warning dialog first
                new AlertDialog.Builder(mContext)
                        .setTitle("Delete")
                        .setMessage("Are you sure you want to delete?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //if yes, begin the delete
                                beginDelete(user.getId());
                            }
                        }).setNegativeButton(android.R.string.no, null).show();

                return false;
            }
        });

    }

    //delete the item
    private void beginDelete(final String userId){
        //show progress dialog
        final ProgressDialog pd = new ProgressDialog(mContext);
        pd.setMessage("Deleting..");
        pd.show();

        //delete from firestore
        firestore.collection("Chatlist").document(mAuth.getUid()).collection("Contacted").document(userId).delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //refresh activity
                        if (mContext instanceof ChatListActivity) {
                            ((ChatListActivity)mContext).getMyUsers();

                        }

                        Toast.makeText(mContext, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }
                });
    }


    @Override
    public int getItemCount() {
        return userList.size();
    }



    public class ViewHolder extends RecyclerView.ViewHolder{

        public TextView username;
        public ImageView profile_image;
        public TextView newMsgsBadge;

        public ViewHolder(View itemView) {
            super(itemView);

            username = itemView.findViewById(R.id.user_username);
            profile_image = itemView.findViewById(R.id.user_profile_image);
            newMsgsBadge = itemView.findViewById(R.id.user_newmgs_tv);

        }
    }
}
