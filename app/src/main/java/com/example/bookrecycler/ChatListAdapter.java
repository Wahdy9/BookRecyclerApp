package com.example.bookrecycler;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

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

        //when click on item go to message activity
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.newMsgsBadge.setVisibility(View.GONE);
                Intent intent = new Intent(mContext, MessageActivity.class);
                intent.putExtra("userId", user.getId());
                mContext.startActivity(intent);
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
        public CircleImageView newMsgsBadge;

        public ViewHolder(View itemView) {
            super(itemView);

            username = itemView.findViewById(R.id.user_username);
            profile_image = itemView.findViewById(R.id.user_profile_image);
            newMsgsBadge = itemView.findViewById(R.id.user_newmgs_iv);

        }
    }
}
