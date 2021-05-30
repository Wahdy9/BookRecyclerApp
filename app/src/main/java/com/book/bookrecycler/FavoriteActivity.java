package com.book.bookrecycler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.ProgressDialog;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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
    private EditText searchET;
    private ProgressDialog pd;

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
        searchET = findViewById(R.id.favorite_search_et);


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
                        try{
                            populateRV();
                        }catch (Exception e){
                            Log.d("FavoriteActivity", "run: " + e.getMessage());
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

    //get items ids from Favorites, and call loadItems.
    private void populateRV() {

        //show progress dialog
        pd = new ProgressDialog(this);
        pd.setMessage("Loading");
        pd.show();

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

        firestore.collection("Items").orderBy("timePosted", Query.Direction.DESCENDING).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
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
                pd.dismiss();
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
