package com.example.bookrecycler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.w3c.dom.Comment;

import java.util.ArrayList;
import java.util.Date;

import javax.annotation.Nullable;

public class ItemDetailsActivity extends AppCompatActivity {


    //Views
    private ImageView imageIV;
    private TextView titleTV, usernameTV, priceTV, conditionTV, categoryTV, descritionTV;
    private EditText commentET;
    private Button sendCommentBtn;

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

        initializeCommentRV();

        //init firebase
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();


        //assign values to view
        Glide.with(this).load(item.getItemImg()).into(imageIV);
        titleTV.setText(item.getTitle());
        usernameTV.setText(getIntent().getStringExtra("username"));
        priceTV.setText(item.getPrice());
        conditionTV.setText(item.getCondition());
        categoryTV.setText(item.getCategory());
        descritionTV.setText(item.getDesc());

        //handle clicks of send comment btn
        sendCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if user is logged, otherwise send him to loginActivity
                if(mAuth.getCurrentUser() != null){
                    sendComment(item.getItemId());
                }else{
                    Toast.makeText(ItemDetailsActivity.this, "You need to login first", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(ItemDetailsActivity.this, LoginAndRegisterActivity.class);
                    startActivity(loginIntent);
                }
            }
        });

        //get comments from firestore and display it in RV
        loadComments(item.getItemId());

    }

    private void sendComment(String itemId) {
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
                        //commentAdapter.notifyDataSetChanged();
                    }
                }
            });

        }else{
            Toast.makeText(this, "Can't send an empty comment", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadComments(String itemId) {
        /*
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
         */

        //get comments from firestore and populate it in th comment recyclerView in realtime
        firestore.collection("Items").document(itemId).collection("Comments")
                .orderBy("timestamp", Query.Direction.ASCENDING).addSnapshotListener(this,new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (!queryDocumentSnapshots.isEmpty()) {
                    //this loop will check if document is added, if so then converted to obj and added to recyclerview, notify adapter
                    for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {
                        if (doc.getType() == DocumentChange.Type.ADDED) {
                            CommentModel comment = doc.getDocument().toObject(CommentModel.class);
                            commentList.add(comment);
                            commentAdapter.notifyDataSetChanged();
                        }
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
}
