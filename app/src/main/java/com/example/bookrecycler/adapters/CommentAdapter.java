package com.example.bookrecycler.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.bookrecycler.R;
import com.example.bookrecycler.Utils;
import com.example.bookrecycler.models.CommentModel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {


    //list to populate
    private ArrayList<CommentModel> commentList;
    private Context mContext;

    //firebase
    private FirebaseFirestore firestore;


    public CommentAdapter(ArrayList<CommentModel> commentList) {
        this.commentList = commentList;
        firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        //get the comment
        CommentModel commentModel = commentList.get(position);

        //set the msg
        holder.textTV.setText(commentModel.getText());

        //get username and image from firestore using userId, then assign them to views
        firestore.collection("Users").document(commentModel.getUser_id()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(documentSnapshot.exists()) {
                    holder.usernameTV.setText(documentSnapshot.getString("name"));

                    //get img
                    String img_url = documentSnapshot.getString("img_url");
                    if (!img_url.equals("default")) {
                        RequestOptions requestOptions = new RequestOptions();//to set the place holder incase if something went wrong
                        requestOptions.placeholder(R.drawable.user_profile);
                        Glide.with(mContext).setDefaultRequestOptions(requestOptions).load(img_url).into(holder.commentImg);
                    }
                }
            }
        });

        //format and set the time
        long time = commentModel.getTimestamp().toDate().getTime();
        CharSequence timePassed = Utils.getTimePassed(time);
        holder.timeTV.setText(timePassed);
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView textTV,usernameTV, timeTV;
        CircleImageView commentImg;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            textTV = itemView.findViewById(R.id.comment_text);
            usernameTV = itemView.findViewById(R.id.comment_username);
            commentImg = itemView.findViewById(R.id.comment_img);
            timeTV = itemView.findViewById(R.id.comment_time);

        }
    }
}
