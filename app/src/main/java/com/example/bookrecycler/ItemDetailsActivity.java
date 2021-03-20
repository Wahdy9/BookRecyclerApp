package com.example.bookrecycler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.bookrecycler.adapters.CommentAdapter;
import com.example.bookrecycler.models.CommentModel;
import com.example.bookrecycler.models.ItemModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.zolad.zoominimageview.ZoomInImageView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ItemDetailsActivity extends AppCompatActivity {


    //Views
    private ZoomInImageView imageIV;
    private ImageView favoriteIV;
    private TextView titleTV, usernameTV, priceTV, conditionTV, categoryTV, descritionTV;
    private EditText commentET;
    private Button sendCommentBtn;
    private LinearLayout chatLL;

    //comment recyclerView
    private RecyclerView commentRV;
    private RecyclerView.Adapter commentAdapter;
    private ArrayList<CommentModel> commentList;

    //firebase
    FirebaseFirestore firestore;
    FirebaseAuth mAuth;

    //the item passed from previous activity
    private ItemModel item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_details);

        //toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //get the item
        item = (ItemModel) getIntent().getSerializableExtra("item");
        //set the title of an item in the toolbar
        getSupportActionBar().setTitle(item.getTitle());

        //init views
        imageIV = findViewById(R.id.item_details_image);
        titleTV = findViewById(R.id.item_details_title);
        usernameTV = findViewById(R.id.item_details_username);
        priceTV = findViewById(R.id.item_details_price);
        conditionTV = findViewById(R.id.item_details_condition);
        categoryTV = findViewById(R.id.item_details_category);
        descritionTV = findViewById(R.id.item_details_description);
        commentET = findViewById(R.id.item_details_comment_et);
        sendCommentBtn = findViewById(R.id.item_details_comment_btn);
        favoriteIV = findViewById(R.id.item_details_favorite);
        chatLL = findViewById(R.id.item_details_chat_ll);


        initializeCommentRV();

        //init firebase
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();


        //assign values to view
        Glide.with(this).load(item.getItemImg()).into(imageIV);
        titleTV.setText(item.getTitle());
        priceTV.setText(item.getPrice());
        conditionTV.setText(item.getCondition());
        categoryTV.setText(item.getCategory());
        descritionTV.setText(item.getDesc());

        //get username from firestore using userId then set the view
        firestore.collection("Users").document(item.getUserId()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    usernameTV.setText(documentSnapshot.getString("name"));
                }
            }
        });

        //handle clicks of send comment btn
        sendCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if user is logged, otherwise send him to loginActivity
                if(mAuth.getCurrentUser() != null){
                    sendComment(item.getItemId());
                }else{
                    Toast.makeText(ItemDetailsActivity.this, "You need to login", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(ItemDetailsActivity.this, LoginAndRegisterActivity.class);
                    startActivity(loginIntent);
                }
            }
        });



        //assign a listner on username that take user to UsersProfileActivity
        usernameTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ItemDetailsActivity.this, UsersProfileActivity.class);
                intent.putExtra("userId", item.getUserId());
                startActivity(intent);
            }
        });


        //handle clicks of favorite
        favoriteIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mAuth.getCurrentUser() != null){
                    //this try to avoid crash when favorite clicked and there is no Internet
                    try {
                        //if user logged, favor or unfavor the item
                        if ((Integer) favoriteIV.getTag() == R.drawable.ic_favorite_grey) {
                            //Favor the item, create a document in favorites and change the icon
                            HashMap<String, Object> map = new HashMap<>();
                            map.put("itemId", item.getItemId());
                            firestore.collection("Users").document(mAuth.getUid()).collection("Favorites")
                                    .document(item.getItemId()).set(map);
                            favoriteIV.setImageResource(R.drawable.ic_favorite_red);
                            favoriteIV.setTag(R.drawable.ic_favorite_red);//tag used in (if), to compare the icon
                            Toast.makeText(ItemDetailsActivity.this, "Item added to favorite", Toast.LENGTH_SHORT).show();
                        } else {
                            //unfavor the item, delete a document in favorites and change the icon
                            firestore.collection("Users").document(mAuth.getUid()).collection("Favorites")
                                    .document(item.getItemId()).delete();
                            favoriteIV.setImageResource(R.drawable.ic_favorite_grey);
                            favoriteIV.setTag(R.drawable.ic_favorite_grey);
                            Toast.makeText(ItemDetailsActivity.this, "Item removed from favorite", Toast.LENGTH_SHORT).show();
                        }
                    }catch (NullPointerException e){
                        Toast.makeText(ItemDetailsActivity.this, "Check your Internet connection", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    //if not logged , send to login activity
                    Toast.makeText(ItemDetailsActivity.this, "You need to login", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(ItemDetailsActivity.this, LoginAndRegisterActivity.class);
                    startActivity(loginIntent);
                }
            }
        });

        //when clicking on chat, send him to MessageActivity to start chatting
        chatLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mAuth.getCurrentUser() != null) {
                    //this (if) is to prevent user to chat with himself
                    if(!mAuth.getCurrentUser().getUid().equals(item.getUserId())) {
                        Intent intent = new Intent(ItemDetailsActivity.this, MessageActivity.class);
                        intent.putExtra("userId", item.getUserId());
                        startActivity(intent);
                    }else{
                        Toast.makeText(ItemDetailsActivity.this, "Can't chat with yourself", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(ItemDetailsActivity.this, "You need to login", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ItemDetailsActivity.this, LoginAndRegisterActivity.class);
                    startActivity(intent);
                }
            }
        });



        //get comments from firestore and display it in RV
        loadComments(item.getItemId());

    }

    //run query to check if user favorite the item
    private void loadFavorite() {
        firestore.collection("Users").document(mAuth.getUid()).collection("Favorites").document(item.getItemId()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    favoriteIV.setImageResource(R.drawable.ic_favorite_red);
                    favoriteIV.setTag(R.drawable.ic_favorite_red);//tag used in (if), to compare the icon
                } else {
                    favoriteIV.setImageResource(R.drawable.ic_favorite_grey);
                    favoriteIV.setTag(R.drawable.ic_favorite_grey);
                }
            }
        });
    }

    private void sendComment(String itemId) {
        //check Internet
        if(!Utils.isConnectedToInternet(ItemDetailsActivity.this)){
            Toast.makeText(ItemDetailsActivity.this, "Check your Internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        //get the comment and check if empty
        String comment = commentET.getText().toString().trim();
        if (!TextUtils.isEmpty(comment)) {
            //generate comment id
            String commentId = firestore.collection("Items").document(itemId).collection("Comments").document().getId();

            //make the comment objext and send to firestore
            final CommentModel commentToSent = new CommentModel( FirebaseAuth.getInstance().getUid(), commentId, comment, new Timestamp(new Date()));
            firestore.collection("Items").document(itemId).collection("Comments").document(commentId).set(commentToSent).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        commentET.setText("");
                        commentList.add(commentToSent);
                        commentAdapter.notifyDataSetChanged();
                    }
                }
            });

        }else{
            Toast.makeText(this, "Can't send an empty comment", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadComments(String itemId) {

        //get comments from firestore and populate it in th comment recyclerView
        firestore.collection("Items").document(itemId).collection("Comments")
                .orderBy("timestamp", Query.Direction.ASCENDING).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (!task.getResult().isEmpty()) {

                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            CommentModel comment = doc.toObject(CommentModel.class);
                            commentList.add(comment);
                        }
                        commentAdapter.notifyDataSetChanged();
                    }
                }
            }
        });


    }

    private void initializeCommentRV() {
        commentList = new ArrayList<>();
        commentRV = findViewById(R.id.item_details_rv);
        commentRV.setHasFixedSize(false);
        commentAdapter = new CommentAdapter(commentList);
        commentRV.setAdapter(commentAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //this method placed in onResume incase if user logged and returned to this activity, it wont crash
        //if user logged, load if he favor the item
        if(mAuth.getCurrentUser() != null){
            loadFavorite();
        }
    }
}
