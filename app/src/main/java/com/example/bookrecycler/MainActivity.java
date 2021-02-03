package com.example.bookrecycler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    //views
    private FloatingActionButton Add_item_fab;
    private ImageButton filterBtn;
    private EditText searchET;
    private ProgressDialog pd;

    //drawer views
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private View headerView;

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    //RecyclerView
    private RecyclerView itemRV;
    private ItemAdapter itemAdapter;
    ArrayList<ItemModel> itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Book Recycler");

        //init views
        Add_item_fab = findViewById(R.id.Add_item_fab);
        filterBtn =  findViewById(R.id.filterBtn);
        searchET = findViewById(R.id.searchET);

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
                        Intent LoginIntent = new Intent(MainActivity.this, LoginAndRegisterActivity.class);
                        startActivity(LoginIntent);
                        break;
                    case R.id.nav_contact:
                        Toast.makeText(MainActivity.this, "Contact", Toast.LENGTH_SHORT).show();
                        break;

                    case R.id.nav_faq:
                        Toast.makeText(MainActivity.this, "FAQ", Toast.LENGTH_SHORT).show();
                        break;
                    ///////////////////////////////////items of logged user layout////////////////////////////////////
                    case R.id.nav_log_profile:
                        startActivity(new Intent(MainActivity.this, MyProfileActivity.class));
                        break;
                    case R.id.nav_log_items:
                        Toast.makeText(MainActivity.this, "Items", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.nav_log_favorite:
                        Toast.makeText(MainActivity.this, "Favorite", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.nav_log_chats:
                        startActivity(new Intent(MainActivity.this, ChatListActivity.class));
                        break;
                    case R.id.nav_log_logout:
                        Toast.makeText(MainActivity.this, "logout from account", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        changesDrawerLayout();
                        populateRV(); //to refresh after sign out
                        break;
                }
                //close drawer after clicking an item
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        //Open AddItemActivity when clicking floating add btn
        Add_item_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if user logged
                if(mAuth.getCurrentUser() != null) {
                    startActivity(new Intent(MainActivity.this, AddItemActivity.class));
                }else{
                    startActivity(new Intent(MainActivity.this, LoginAndRegisterActivity.class));
                }
            }
        });

        //Open bottomsheet for filtering
        filterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterBottomSheet();
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

        //initialize & populate RV
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
        itemAdapter.filterList(filteredList);
    }




    //load the drawer Layout based on user if he logged or guest
    private void changesDrawerLayout() {
        //check if user logged, if so load the logged drawer layout
        if(mAuth.getCurrentUser() != null){
            //this (if) to prevent duplicate nav header
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

    //init recyclerView
    private void initializeRV() {
        itemList = new ArrayList<>();
        itemRV = findViewById(R.id.main_items_rv);
        itemRV.setHasFixedSize(true);
        itemRV.setNestedScrollingEnabled(true);
        itemAdapter = new ItemAdapter(itemList);
        itemRV.setAdapter(itemAdapter);
    }

    //get data from firestore and populate the recyclerView
    private void populateRV() {
        itemList.clear();//to avoid repetition

        //show progress dialog
        pd = new ProgressDialog(this);
        pd.setMessage("Loading");
        pd.show();

        //show in the filter option in the toolbar
        getSupportActionBar().setSubtitle( "Category:"+ selectedCategory+ ", Condition:" + selectedCondition);


        //send query to firestore DEPENDING of the filters, add the items them to recyclerview.
        if(selectedCategory.equalsIgnoreCase("All") && //if both category and condition are All
                selectedCondition.equalsIgnoreCase("All")) {

            firestore.collection("Items").orderBy("timePosted", Query.Direction.DESCENDING).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                    if (queryDocumentSnapshots != null) {
                        //get all items, add them to itemList
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            ItemModel item = doc.toObject(ItemModel.class);
                            itemList.add(item);
                        }
                        //notify adapter
                        //itemAdapter.notifyDataSetChanged();
                        itemAdapter = new ItemAdapter(itemList);
                        itemRV.setAdapter(itemAdapter);

                        pd.dismiss();
                    }

                }
            });
        }else if(!selectedCategory.equalsIgnoreCase("All") && //if category is not All
                selectedCondition.equalsIgnoreCase("All")){

            firestore.collection("Items").whereEqualTo("category", selectedCategory).orderBy("timePosted", Query.Direction.DESCENDING).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                    if (queryDocumentSnapshots != null) {
                        //get all items, add them to itemList
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            ItemModel item = doc.toObject(ItemModel.class);
                            itemList.add(item);
                        }
                        //notify adapter
                        //itemAdapter.notifyDataSetChanged();
                        itemAdapter = new ItemAdapter(itemList);
                        itemRV.setAdapter(itemAdapter);

                        pd.dismiss();
                    }

                }
            });

        }else if(selectedCategory.equalsIgnoreCase("All") &&  //if condition is not All
                !selectedCondition.equalsIgnoreCase("All")){

            firestore.collection("Items").whereEqualTo("condition", selectedCondition).orderBy("timePosted", Query.Direction.DESCENDING).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                    if (queryDocumentSnapshots != null) {
                        //get all items, add them to itemList
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            ItemModel item = doc.toObject(ItemModel.class);
                            itemList.add(item);
                        }
                        //notify adapter
                        //itemAdapter.notifyDataSetChanged();
                        itemAdapter = new ItemAdapter(itemList);
                        itemRV.setAdapter(itemAdapter);

                        pd.dismiss();
                    }

                }
            });

        }else{ //if both category and condition are not All

            firestore.collection("Items").whereEqualTo("category", selectedCategory).whereEqualTo("condition", selectedCondition).orderBy("timePosted", Query.Direction.DESCENDING).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                    if (queryDocumentSnapshots != null) {
                        //get all items, add them to itemList
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            ItemModel item = doc.toObject(ItemModel.class);
                            itemList.add(item);
                        }
                        //notify adapter
                        //itemAdapter.notifyDataSetChanged();
                        itemAdapter = new ItemAdapter(itemList);
                        itemRV.setAdapter(itemAdapter);

                        pd.dismiss();
                    }

                }
            });
        }

    }

    //init selected items(by default All) -> these variables used to save the options user selected in the filtring, and used for sending queries
    private String selectedCategory = "All", selectedCondition = "All";
    private int selectedCategoryPosition = 0, selectedConditionPosition=0;
    //setup and display bottomsheet
    private void filterBottomSheet() {
        //inflate (filter_bottom_layout) and its views for bottom sheet
        View view = LayoutInflater.from(this).inflate(R.layout.filter_bottom_layout, null);
        Spinner categorySpinner = view.findViewById(R.id.bottom_sheet_category_spinner);
        Spinner conditionSpinner = view.findViewById(R.id.bottom_sheet_condition_spinner);
        Button applyBtn = view.findViewById(R.id.bottom_sheet_apply_btn);

        //create an array adapter using the string array and default spinner layout
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,Constants.CATEGORIES);
        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,Constants.CONDITIONS);
        //specify the layout ti use when list of choices appears
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //apply adapters to spinners
        categorySpinner.setAdapter(categoryAdapter);
        conditionSpinner.setAdapter(conditionAdapter);
        //set the last selected value
        categorySpinner.setSelection(selectedCategoryPosition);
        conditionSpinner.setSelection(selectedConditionPosition);

        //spinner item selected listener
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = Constants.CATEGORIES[position];
                selectedCategoryPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        conditionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCondition = Constants.CONDITIONS[position];
                selectedConditionPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //setup bottom sheet dialog
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        //add layout to bottom sheet dialog
        bottomSheetDialog.setContentView(view);
        //show the dialog
        bottomSheetDialog.show();

        //setup applyBtn
        applyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();

                populateRV();
            }
        });
    }

    //this method for when click back btn it will close the drawer if open not the activity
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
