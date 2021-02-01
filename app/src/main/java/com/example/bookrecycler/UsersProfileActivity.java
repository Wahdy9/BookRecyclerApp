package com.example.bookrecycler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class UsersProfileActivity extends AppCompatActivity {

    //views
    private ImageView profileImg;
    private TextView usernameTV, majorTV,itemCountTV;
    private LinearLayout chatLL;

    //firebase
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    //Recyclerview
    private RecyclerView itemRV;
    private RecyclerView.Adapter itemAdapter;
    private ArrayList<ItemModel> itemList;

    private String userId;//passed from ItemDetailsActivity, used to query that user information

    private String name;//used to set the name of user in the toolbar title


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_profile);

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

        //get user id from the intent
        userId = getIntent().getStringExtra("userId");

        //inilalize views
        profileImg = findViewById(R.id.userProfile_img);
        usernameTV = findViewById(R.id.userProfile_username_tv);
        majorTV = findViewById(R.id.userProfile_major_tv);
        itemCountTV = findViewById(R.id.userProfile_count_tv);
        chatLL = findViewById(R.id.userProfile_chat_ll);

        //inilalize firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();


        //when clicking on chat, send him to MessageActivity to start chatting
        chatLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mAuth.getCurrentUser() != null) {
                    Toast.makeText(UsersProfileActivity.this, "Chat activity not imepelemted yet", Toast.LENGTH_SHORT).show();
                    //Intent intent = new Intent(UsersProfileActivity.this, MessageActivity.class);
                    //intent.putExtra("userId", userId);
                    //startActivity(intent);
                }else{
                    Toast.makeText(UsersProfileActivity.this, "You need to login..", Toast.LENGTH_LONG).show();
                    //Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
                    //startActivity(intent);
                }
            }
        });

        loadUserInfo();
    }


    //load info from firestore and assign them to views
    private void loadUserInfo() {
        firestore.collection("Users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(!task.isSuccessful()){
                    Toast.makeText(UsersProfileActivity.this, "error:" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                //get info and assign them to views
                if(task.getResult() != null) {
                    name = task.getResult().getString("name");
                    String major = task.getResult().getString("major");
                    String profileImgUrl = task.getResult().getString("img_url");
                    if (name != null) {
                        usernameTV.setText(name);
                        getSupportActionBar().setTitle(name + " Profile");
                    }
                    if(major!= null) {
                        majorTV.setText(major);
                    }
                    if(profileImgUrl != null){
                        //assign image
                        if (!profileImgUrl.equals("default")) {
                            RequestOptions requestOptions = new RequestOptions();
                            requestOptions.placeholder(R.drawable.user_profile);
                            Glide.with(UsersProfileActivity.this).setDefaultRequestOptions(requestOptions).load(profileImgUrl).into(profileImg);

                        }
                    }

                    initializeRV();
                }
            }
        });
    }

    private void initializeRV() {
        itemList = new ArrayList<>();
        itemRV = findViewById(R.id.userProfile_rv);
        itemAdapter = new UsersProfileAdapter(itemList, name);
        itemRV.setAdapter(itemAdapter);

        populateRV();
    }

    private void populateRV() {
        itemList.clear();//clear the list to avoid duplicate

        //get items from firestore and populate it in the recyclerview
        firestore.collection("Items").whereEqualTo("userId", userId).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                if (queryDocumentSnapshots != null) {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        ItemModel post = doc.toObject(ItemModel.class);
                        itemList.add(post);
                    }
                    itemCountTV.setText(""+ itemList.size());
                    itemAdapter.notifyDataSetChanged();
                }

            }
        });
    }
}
