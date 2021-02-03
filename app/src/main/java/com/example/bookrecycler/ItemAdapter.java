package com.example.bookrecycler;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import javax.annotation.Nullable;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder>{

    //list to populate
    private ArrayList<ItemModel> itemList;

    //firebase
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
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

        //set title
        holder.titleTV.setText(itemList.get(position).title);

        //get username from firestore using userId then set the view
        firestore.collection("Users").document(itemList.get(position).userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
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
        firestore.collection("Items").document(itemList.get(position).getItemId()).collection("Comments").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (!queryDocumentSnapshots.isEmpty()) {//if no comments
                    int commentCount = queryDocumentSnapshots.size();
                    holder.commentCountTV.setText(commentCount + "");
                } else {
                    holder.commentCountTV.setText(0 + "");
                }
            }
        });

        //set item image and add a placeholder, just incase
        RequestOptions requestOptions = new RequestOptions().placeholder(R.color.colorGrey2);
        Glide.with(mContext).setDefaultRequestOptions(requestOptions).load(itemList.get(position).itemImg).into(holder.imgIV);

        //set category
        holder.categoryTV.setText(itemList.get(position).category);

        //if item belong to current user, show more btn, so user can edit or delete item
        if(mAuth.getCurrentUser() != null) {
            if (mAuth.getCurrentUser().getUid().equals(itemList.get(holder.getAdapterPosition()).getUserId())) {
                holder.moreIV.setVisibility(View.VISIBLE);
                holder.moreIV.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //show popup menu with two options, edit and delete
                        //create options
                        PopupMenu popupMenu = new PopupMenu(mContext, holder.moreIV, Gravity.END);

                        //add delete and edit entries in the menu
                        popupMenu.getMenu().add(Menu.NONE, 0,0,"Delete");
                        popupMenu.getMenu().add(Menu.NONE, 1,0,"Edit");

                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                int id = item.getItemId();
                                if(id==0){
                                    //delete item
                                    //Show warning dialog first
                                    new AlertDialog.Builder(mContext)
                                            .setTitle("Delete")
                                            .setMessage("Are you sure you want to delete?")
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    //if yes, begin the delete
                                                    beginDelete(itemList.get(position).getItemId(), itemList.get(position).getItemImg());
                                                }
                                            }).setNegativeButton(android.R.string.no, null).show();

                                }else if(id ==1){
                                    //edit item
                                    //start AddPostActivity with extras:edit_mode & itemToEdit
                                    Intent intent = new Intent(mContext, AddItemActivity.class);
                                    intent.putExtra("edit_mode", true);
                                    intent.putExtra("item_to_edit", itemList.get(position));
                                    mContext.startActivity(intent);
                                }

                                return false;
                            }
                        });

                        //show popup menu
                        popupMenu.show();
                    }
                });
            }
        }


        //handle clicks on item
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //go to itemDetailActivity
                Intent intent = new Intent(v.getContext(), ItemDetailsActivity.class);
                intent.putExtra("item", itemList.get(position));
                v.getContext().startActivity(intent);

            }
        });

    }

    //delete the item
    private void beginDelete(final String itemId, String itemImg) {
        final ProgressDialog pd = new ProgressDialog(mContext);
        pd.setMessage("Deleting..");
        pd.show();
        /*
        steps:
            1- delete img from storage
            2- delete item from firestore
         */
        StorageReference imgRef = FirebaseStorage.getInstance().getReferenceFromUrl(itemImg);
        imgRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                firestore.collection("Items").document(itemId).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //refresh the RV in the Main Activity
                        if (mContext instanceof MainActivity) {
                            ((MainActivity)mContext).populateRV();
                        }

                        Toast.makeText(mContext, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //fail to delete img
                pd.dismiss();
                Toast.makeText(mContext, "Error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
