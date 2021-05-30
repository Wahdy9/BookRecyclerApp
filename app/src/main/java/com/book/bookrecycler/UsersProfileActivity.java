package com.book.bookrecycler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.book.bookrecycler.adapters.UsersProfileAdapter;
import com.book.bookrecycler.models.ItemModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UsersProfileActivity extends AppCompatActivity {

    //views
    private ImageView profileImg;
    private TextView usernameTV, provinceTV,itemCountTV;
    private LinearLayout chatLL, phoneLL, emailLL;
    private RatingBar ratingBar;
    private TextView avgRatingTV;
    private ProgressDialog pd;

    //firebase
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    //Recyclerview
    private RecyclerView itemRV;
    private RecyclerView.Adapter itemAdapter;
    private ArrayList<ItemModel> itemList;

    private String userId;//passed from ItemDetailsActivity, used to query that user information

    private String name, phone,email;//used to set the name of user in the toolbar title


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
        provinceTV = findViewById(R.id.userProfile_province_tv);
        itemCountTV = findViewById(R.id.userProfile_count_tv);
        chatLL = findViewById(R.id.userProfile_chat_ll);
        phoneLL = findViewById(R.id.userProfile_phone_ll);
        emailLL = findViewById(R.id.userProfile_email_ll);
        ratingBar = findViewById(R.id.userProfile_rating_bar);
        avgRatingTV =  findViewById(R.id.userProfile_avg_rating_tv);

        //inilalize firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();


        //when clicking on chat, send him to MessageActivity to start chatting
        chatLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if user logged, if not send to login activity
                if(mAuth.getCurrentUser() != null) {
                    //this (if) is to prevent user to chat with himself
                    if(!mAuth.getCurrentUser().getUid().equals(userId)) {
                        Intent intent = new Intent(UsersProfileActivity.this, MessageActivity.class);
                        intent.putExtra("userId", userId);
                        startActivity(intent);
                    }else{
                        Toast.makeText(UsersProfileActivity.this, "Can't chat with yourself", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(UsersProfileActivity.this, "You need to login", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(UsersProfileActivity.this, LoginAndRegisterActivity.class);
                    startActivity(intent);
                }
            }
        });

        //when clicking on phone, send him to phone dial
        phoneLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if user logged, if not send to login activity
                if(mAuth.getCurrentUser() != null) {
                    //send to phone
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + phone));
                    //to make sure app for this intent exist
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }

                }else{
                    Toast.makeText(UsersProfileActivity.this, "You need to login", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(UsersProfileActivity.this, LoginAndRegisterActivity.class);
                    startActivity(intent);
                }
            }
        });

        //when clicking on email, send him to email app
        emailLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if user logged, if not send to login activity
                if(mAuth.getCurrentUser() != null) {
                    String [] addresses = {email};//address to send email to

                    //show what email app to open
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                    intent.putExtra(Intent.EXTRA_EMAIL, addresses);
                    //to make sure app for this intent exist, so it won't crash
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }

                }else{
                    Toast.makeText(UsersProfileActivity.this, "You need to login", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(UsersProfileActivity.this, LoginAndRegisterActivity.class);
                    startActivity(intent);
                }
            }
        });


        setupRating();

        loadUserInfo();
    }

    //load and setup the rating
    private void setupRating() {
        //check if user logged in and profile not belong to the current user
        if(mAuth.getCurrentUser()!= null && !mAuth.getUid().equalsIgnoreCase(userId)){
            //get the rate of current logged user and assign it to the rating bar
            firestore.collection("Users").document(userId).collection("Ratings").
                    document(mAuth.getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()){
                        double stars = (double)documentSnapshot.get("stars");
                        ratingBar.setRating((float)stars);
                    }
                }
            });

            ///Listener called when there is a change in the rating bar
            ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                @Override
                public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                    //create rating map
                    Map<String, Object> ratingMap = new HashMap<>();
                    ratingMap.put("userId", mAuth.getUid());
                    ratingMap.put("stars", ratingBar.getRating());
                    //upload to firestore
                    firestore.collection("Users").document(userId).collection("Ratings")
                            .document(mAuth.getUid()).set(ratingMap);
                    //refresh the average rating textView
                    loadAverageRating();
                }
            });

        }else{
            //hide the rating, when user enter his profile OR Guest enter
            ratingBar.setVisibility(View.GONE);
        }
        //get the avg rating
        loadAverageRating();
    }

    //get the avg rating, assign it to average textview
    private void loadAverageRating(){
        firestore.collection("Users").document(userId).collection("Ratings").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if (queryDocumentSnapshots != null) {
                    double sum = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        sum = sum + (double)doc.get("stars");
                    }

                    int noOfRatings = queryDocumentSnapshots.getDocuments().size();
                    if(noOfRatings==0){
                        //if there is no rating, make it 0/5
                        avgRatingTV.setText("0/5");
                    }else{
                        //if there is rating, get the avg
                        avgRatingTV.setText(String.format("%.1f", (sum/noOfRatings)) + "/5");
                    }
                }
            }
        });
    }

    //load info from firestore and assign them to views
    private void loadUserInfo() {
        //show progress dialog
        pd = new ProgressDialog(this);
        pd.setMessage("Loading");
        pd.show();

        firestore.collection("Users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(!task.isSuccessful()){
                    Toast.makeText(UsersProfileActivity.this, "Something went wrong\n  please try again later" , Toast.LENGTH_LONG).show();
                    Log.d("UsersProfileActivity", "onComplete: " + task.getException().getMessage());
                    return;
                }
                //get info and assign them to views
                if(task.getResult() != null) {
                    name = task.getResult().getString("name");
                    String province = task.getResult().getString("province");
                    String country = task.getResult().getString("country");
                    email = task.getResult().getString("email");
                    phone = task.getResult().getString("phone");
                    boolean showEmail = task.getResult().getBoolean("showEmail");
                    boolean showPhone = task.getResult().getBoolean("showPhone");
                    String profileImgUrl = task.getResult().getString("img_url");

                    if (name != null) {
                        usernameTV.setText(name);
                        getSupportActionBar().setTitle(name + "'s Profile");
                    }
                    if(country!= null ) {
                        if(province !=null)
                        provinceTV.setText(province +"-"+country);
                        else
                            provinceTV.setText(country);
                    }
                    if(!showEmail){
                        emailLL.setVisibility(View.GONE);
                    }
                    if(!showPhone){
                        phoneLL.setVisibility(View.GONE);
                    }
                    if(profileImgUrl != null){
                        //assign image
                        if (!profileImgUrl.equals("default")) {
                            RequestOptions requestOptions = new RequestOptions().placeholder(R.drawable.user_profile);
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
                pd.dismiss();

            }
        });
    }
}
