package com.example.bookrecycler.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bookrecycler.ItemDetailsActivity;
import com.example.bookrecycler.R;
import com.example.bookrecycler.models.ItemModel;

import java.util.ArrayList;

public class UsersProfileAdapter extends RecyclerView.Adapter<UsersProfileAdapter.ViewHolder>{

    //list to populate
    private ArrayList<ItemModel> itemList;
    private Context mContext;

    String username;//we get it from UserProfileActivity so we dont have to send a query, we will pass it to ItemDetailsActivity

    public UsersProfileAdapter(ArrayList<ItemModel> itemList, String username) {
        this.itemList = itemList;
        this.username = username;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_userprofile, parent, false);
        return new UsersProfileAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {

        //setup the imgs
        Glide.with(mContext).load(itemList.get(position).getItemImg()).into(holder.imgIV);

        //handle clicks on item
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //go to itemDetailActivity
                Intent intent = new Intent(v.getContext(), ItemDetailsActivity.class);
                intent.putExtra("item", itemList.get(position));
                intent.putExtra("username", username);//we send username here, to avoid sending another query for it
                v.getContext().startActivity(intent);

            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    //view holder
    class ViewHolder extends RecyclerView.ViewHolder {

        View itemView;
        ImageView imgIV;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;

            //initailize views
            imgIV = itemView.findViewById(R.id.image_item);

        }
    }
}
