package com.example.bookrecycler;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
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
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import javax.annotation.Nullable;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder>{

    private ArrayList<ItemModel> itemList;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    private Context mContext;


    public ItemAdapter(ArrayList<ItemModel> itemList) {
        this.itemList = itemList;
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        //set title
        holder.titleTV.setText(itemList.get(position).title);

        //set username
        firestore.collection("Users").document(itemList.get(position).userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    //username = documentSnapshot.getString("name");
                    holder.usernameTV.setText(documentSnapshot.getString("name"));
                }
            }
        });

        //format and set time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        long time = itemList.get(position).timePosted.getTime();
        long now = System.currentTimeMillis();
        CharSequence ago = DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS);
        holder.timeTV.setText(ago);

        //get comment counts
        firestore.collection("Posts").document(itemList.get(position).getItemId()).collection("Comments").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (!queryDocumentSnapshots.isEmpty()) {
                    int commentCount = queryDocumentSnapshots.size();
                    holder.commentCountTV.setText(commentCount + "");
                } else {
                    holder.commentCountTV.setText(0 + "");
                }
            }
        });

        //set item image
        Glide.with(mContext).load(itemList.get(position).itemImg).into(holder.imgIV);

        //set category
        holder.categoryTV.setText(itemList.get(position).category);

        //if item belong to current user, show more btn, so user can edit or delete item
        if(mAuth.getCurrentUser().getUid().equals(itemList.get(position).getUserId())){
            holder.moreIV.setVisibility(View.VISIBLE);
            holder.moreIV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }


        //handle clicks on item
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //go to itemDetailsActivity

                /*Intent intent = new Intent(v.getContext(), ItemDetailsActivity.class);
                intent.putExtra("item", itemList.get(position));
                //intent.putExtra("username", username);
                v.getContext().startActivity(intent);
                */
            }
        });

    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }




    class ViewHolder extends RecyclerView.ViewHolder {

        View itemView;
        ImageView imgIV, moreIV;
        TextView titleTV, usernameTV, timeTV, commentCountTV,categoryTV;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;

            //initailize views
            titleTV = itemView.findViewById(R.id.titleLB);
            usernameTV = itemView.findViewById(R.id.usernameLB);
            timeTV = itemView.findViewById(R.id.timeLB);
            commentCountTV = itemView.findViewById(R.id.commentCountLB);
            imgIV = itemView.findViewById(R.id.imgLB);
            categoryTV = itemView.findViewById(R.id.categoryLB);
            moreIV = itemView.findViewById(R.id.moreLB);
        }
    }
}
