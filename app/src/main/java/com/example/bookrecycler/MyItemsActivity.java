package com.example.bookrecycler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class MyItemsActivity extends AppCompatActivity {

    //Recyclerview
    private RecyclerView itemRV;
    private RecyclerView.Adapter itemAdapter;
    private ArrayList<ItemModel> itemList;

    //Views
    private SwipeRefreshLayout refreshLayout;
    private TextView notFoundTV;
    private FloatingActionButton Add_item_fab;


    //Fireabse
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_items);

        //Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("My Items");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //iniatilize views
        refreshLayout = findViewById(R.id.my_items_refresh_layout);
        notFoundTV =findViewById(R.id.my_items_found_tv);
        Add_item_fab = findViewById(R.id.my_item_fab);


        //iniatilize firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        //Open AddItemActivity when clicking floating add btn
        Add_item_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MyItemsActivity.this, AddItemActivity.class));
            }
        });

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

    //get items from firestore, add them to recyclerview.
    private void populateRV() {
        itemList.clear();
        firestore.collection("Items").whereEqualTo("userId", mAuth.getUid()).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                if (queryDocumentSnapshots != null) {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        ItemModel post = doc.toObject(ItemModel.class);
                        itemList.add(post);
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
        itemRV = findViewById(R.id.my_items_rv);
        itemAdapter = new ItemAdapter(itemList);
        itemRV.setAdapter(itemAdapter);
    }
}
