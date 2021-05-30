package com.book.bookrecycler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.book.bookrecycler.adapters.ItemAdapter;
import com.book.bookrecycler.models.ItemModel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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
    private EditText searchET;
    private ProgressDialog pd;

    //Fireabse
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;

    //boolean used to refresh the MyItemsActivity when there are changes that affects it in another activity
    public static boolean refreshMyItemsActivity;

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
        searchET = findViewById(R.id.my_items_search_et);


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
                        try {
                            populateRV();
                        }catch (Exception e){
                            Log.d("MyItemsActivity", "run: " + e.getMessage());
                        }

                    }
                }, 2000);
            }
        });


        //add text listener to filter
        searchET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());

            }
        });

        //set to false so it wont refresh when activity created, no point of refreshing
        refreshMyItemsActivity = false;

        initializeRV();

        populateRV();
    }

    //this method used to filter RV when typing in search field
    private void filter(String text) {
        ArrayList<ItemModel> filteredList = new ArrayList<>();

        for (int i = 0; i < itemList.size(); i++) {
            if (itemList.get(i).getTitle().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(itemList.get(i));
            }
        }
        itemAdapter = new ItemAdapter(filteredList);
        itemRV.setAdapter(itemAdapter);
    }

    //get items from fireStore, add them to recyclerView.
    public void populateRV() {

        //show progress dialog
        pd = new ProgressDialog(this);
        pd.setMessage("Loading");
        pd.show();

        itemList.clear();
        firestore.collection("Items").whereEqualTo("userId", mAuth.getUid()).orderBy("timePosted", Query.Direction.DESCENDING).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                if (queryDocumentSnapshots != null) {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        ItemModel post = doc.toObject(ItemModel.class);
                        itemList.add(post);
                    }
                    itemAdapter.notifyDataSetChanged();//try to change to the other notifyer is better

                    //if no item found in the favorite, show notFoundTV
                    if(itemList.size()==0){
                        notFoundTV.setVisibility(View.VISIBLE);
                    }else{
                        notFoundTV.setVisibility(View.GONE);// no need for this
                    }
                }
                pd.dismiss();

            }
        });
    }

    private void initializeRV() {
        itemList = new ArrayList<>();
        itemRV = findViewById(R.id.my_items_rv);
        itemAdapter = new ItemAdapter(itemList);
        itemRV.setAdapter(itemAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //if changes happen in another activity, it will refresh the activity
        if(refreshMyItemsActivity){
            populateRV();
            refreshMyItemsActivity = false;
        }
    }
}
