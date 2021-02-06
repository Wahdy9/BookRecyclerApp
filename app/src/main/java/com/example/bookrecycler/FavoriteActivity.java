package com.example.bookrecycler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class FavoriteActivity extends AppCompatActivity {

    //Recyclerview
    private RecyclerView itemRV;
    private RecyclerView.Adapter itemAdapter;
    private ArrayList<ItemModel> itemList;

    //Views
    private SwipeRefreshLayout refreshLayout;
    private TextView notFoundTV;

    //Fireabse
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        //Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Favorite");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //iniatilize views
        refreshLayout = findViewById(R.id.favorite_refresh_layout);
        notFoundTV =findViewById(R.id.favorite_found_tv);

        //iniatilize firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        //setup refresh layout
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //stop refreshing
                        refreshLayout.setRefreshing(false);
                        //repopulate recycler view
                        populateRV();
                    }
                }, 3000);
            }
        });

        initializeRV();

        populateRV();
    }

    //get items ids from Favorites, and call getPosts.
    private void populateRV() {

        //temp array to hold the ids of items user favorite
        final ArrayList<String> favoritePostIdsList = new ArrayList<>();

        itemList.clear();//to avoid duplicate

        //get ids from favorite
        firestore.collection("Users").document(mAuth.getUid()).collection("Favorites").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if (queryDocumentSnapshots != null){
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String id = doc.getId();
                        favoritePostIdsList.add(id);
                    }
                    loadItems(favoritePostIdsList);
                }
            }
        });

    }


    //get the items and compare them with ids list and add them to RV
    private void loadItems(final ArrayList<String> idsList) {

        firestore.collection("Items").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if (queryDocumentSnapshots != null) {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        ItemModel item = doc.toObject(ItemModel.class);
                        if(idsList.contains(item.getItemId())){

                            itemList.add(item);
                        }
                    }
                    itemAdapter.notifyDataSetChanged();

                    //if no item found in the favorite, show notFoundTV
                    if(itemList.size()==0){
                        notFoundTV.setVisibility(View.VISIBLE);
                    }else{
                        notFoundTV.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private void initializeRV() {
        itemList = new ArrayList<>();
        itemRV = findViewById(R.id.favorite_rv);
        itemAdapter = new ItemAdapter(itemList);
        itemRV.setAdapter(itemAdapter);
    }

}
