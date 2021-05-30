package com.book.bookrecycler.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.book.bookrecycler.R;
import com.book.bookrecycler.UsersProfileActivity;
import com.book.bookrecycler.Utils;
import com.book.bookrecycler.models.CommentModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
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
    private FirebaseAuth mAuth;


    public CommentAdapter(ArrayList<CommentModel> commentList) {
        this.commentList = commentList;
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

        //get the comment
        final CommentModel commentModel = commentList.get(position);

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

        //send to profile when clicking username
        holder.usernameTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, UsersProfileActivity.class);
                intent.putExtra("userId", commentModel.getUser_id());
                mContext.startActivity(intent);
            }
        });

        //long click on comment by owner, show delete dialog to delete comment
        if(mAuth.getCurrentUser() != null) {
            if(commentModel.getUser_id().equals(mAuth.getCurrentUser().getUid())) {
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
                                        //check Internet first
                                        if(!Utils.isConnectedToInternet(mContext)){
                                            Toast.makeText(mContext, "Check your Internet connection", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        //start deleting the comment
                                        deleteComment(commentModel);

                                    }
                                }).setNegativeButton(android.R.string.no, null).show();

                        return false;
                    }
                });
            }
        }
    }

    private void deleteComment(final CommentModel commentModel) {
        //show progress dialog
        final ProgressDialog pd = new ProgressDialog(mContext);
        pd.setMessage("Deleting..");
        pd.show();

        //delete from firestore
        firestore.collection("Items").document(commentModel.getCommentItemId())
                .collection("Comments").document(commentModel.getCommentId()).delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //delete from list and refresh adapter
                        if(commentList.remove(commentModel)){
                            notifyDataSetChanged();
                            Toast.makeText(mContext, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                            pd.dismiss();
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(mContext, "Something went wrong\n  please try again later", Toast.LENGTH_LONG).show();
                Log.d("CommentAdapter", "onFailure(Delete image): " + e.getMessage());
            }
        });
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
