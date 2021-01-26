package com.example.bookrecycler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    //drawer views
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private View headerView;

    //firebase
    private FirebaseAuth mAuth;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Book Recycler");

        //initialize firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        //initialize drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        headerView= LayoutInflater.from(this).inflate(R.layout.nav_header, null);

        //to show the hamburger menu and sync it with the drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,  R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        //click listener for the drawer items
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()){
                    /////////////////////////////////items of Guest user layout/////////////////////////////////////
                    case R.id.nav_def_signin:
                        Intent intent = new Intent(getApplicationContext(), LoginAndRegisterActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.nav_contact:
                        Toast.makeText(MainActivity.this, "Contact", Toast.LENGTH_SHORT).show();
                        break;

                    case R.id.nav_faq:
                        Toast.makeText(MainActivity.this, "FAQ", Toast.LENGTH_SHORT).show();
                        break;
                    ///////////////////////////////////items of logged user layout////////////////////////////////////
                    case R.id.nav_log_profile:
                        Toast.makeText(MainActivity.this, "Profile", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.nav_log_posts:
                        Toast.makeText(MainActivity.this, "Items", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.nav_log_favorite:
                        Toast.makeText(MainActivity.this, "Favorite", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.nav_log_chats:
                        Toast.makeText(MainActivity.this, "Chats", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.nav_log_logout:
                        Toast.makeText(MainActivity.this, "logout from account", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        changesDrawerLayout();
                        break;
                }
                //close drawer after clicking an item
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

    }




    //load the drawerlayout based on user if he logged or guest
    private void changesDrawerLayout() {
        //check if user logged, if so load the logged drawer layout
        if(mAuth.getCurrentUser() != null){
            //this (f) to prevent duplicate nav header
            if(navigationView.getHeaderCount() == 0) {
                navigationView.addHeaderView(headerView);
                //get user info and assign it to header views
                final TextView headerTV = headerView.findViewById(R.id.nav_header_tv);
                final CircleImageView headerIV = headerView.findViewById(R.id.nav_header_iv);
                firestore.collection("Users").document(mAuth.getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        headerTV.setText(documentSnapshot.getString("name"));

                        String profileImgUrl = documentSnapshot.getString("img_url");
                        if (!profileImgUrl.equals("default")) {
                            RequestOptions requestOptions = new RequestOptions();
                            requestOptions.placeholder(R.drawable.user_profile);
                            Glide.with(MainActivity.this).setDefaultRequestOptions(requestOptions).load(profileImgUrl).into(headerIV);
                        }else{
                            Glide.with(MainActivity.this).load(R.drawable.user_profile).into(headerIV);
                        }
                    }
                });
                //load the drawer layout of logged user
                navigationView.getMenu().clear();
                navigationView.inflateMenu(R.menu.navigation_drawer_logged_users);
            }
        }else{
            //load the drawer layout of guest users
            navigationView.removeHeaderView(headerView);
            navigationView.getMenu().clear();
            navigationView.inflateMenu(R.menu.navigation_drawer_guest_users);
        }
    }


    //this method for when click back btn it will close the drawer not activity
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    //Change drawer when user return from login activity
    @Override
    protected void onResume() {
        super.onResume();
        changesDrawerLayout();
    }
}
