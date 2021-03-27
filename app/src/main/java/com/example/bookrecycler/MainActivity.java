package com.example.bookrecycler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
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
import com.example.bookrecycler.adapters.ItemAdapter;
import com.example.bookrecycler.models.ItemModel;
import com.example.bookrecycler.notification.Token;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    //views
    private SwipeRefreshLayout refreshLayout;
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
    private ArrayList<ItemModel> itemList;

    //boolean to check for new msgs, so we can show the new msgs badge
    boolean newMsgs = false;

    //boolean used to refresh the MainActivity when there are changes that affects it in another activity
    public static boolean refreshMainActivity = false;

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Book Recycler");

        //init views
        refreshLayout = findViewById(R.id.main_refresh_layout);
        FloatingActionButton add_item_fab = findViewById(R.id.Add_item_fab);
        ImageButton filterBtn = findViewById(R.id.filterBtn);
        searchET = findViewById(R.id.searchET);

        //initialize firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        //initialize drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        headerView= LayoutInflater.from(this).inflate(R.layout.nav_header, null);

        //this will make icons in drawer display their original color
        navigationView.setItemIconTintList(null);

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
                        Intent ContactUs = new Intent(MainActivity.this,Contac_Us.class);
                        startActivity(ContactUs);
                        break;
                    case R.id.nav_about_book:
                        Intent About = new Intent(MainActivity.this,About_Page.class);
                        startActivity(About);
                        break;
                    ///////////////////////////////////items of logged user layout////////////////////////////////////
                    case R.id.nav_log_profile:
                        startActivity(new Intent(MainActivity.this, MyProfileActivity.class));
                        break;
                    case R.id.nav_log_items:
                        startActivity(new Intent(MainActivity.this, MyItemsActivity.class));
                        break;
                    case R.id.nav_log_favorite:
                        startActivity(new Intent(MainActivity.this, FavoriteActivity.class));
                        break;
                    case R.id.nav_log_chats:
                        newMsgs = false;
                        startActivity(new Intent(MainActivity.this, ChatListActivity.class));
                        break;
                    case R.id.nav_log_logout:
                        newMsgs = false;
                        Toast.makeText(MainActivity.this, "You logged out from your account", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        changesDrawerLayout();
                        populateRV(); //to refresh after sign out
                        break;
                }
                //close drawer after clicking an item with an animation called gradvityCompat
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        //Open AddItemActivity when clicking floating add btn
        add_item_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if user logged
                if(mAuth.getCurrentUser() != null) {
                    startActivity(new Intent(MainActivity.this, AddItemActivity.class));
                }else{
                    startActivity(new Intent(MainActivity.this, LoginAndRegisterActivity.class));
                    Toast.makeText(MainActivity.this, "You Must Login or Register to Upload an Item", Toast.LENGTH_SHORT).show();
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
                //No need of it for now
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need for it now
            }

            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());

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
                        //refresh activity
                        refreshActivity();
                    }
                }, 2000);
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
        itemAdapter = new ItemAdapter(filteredList);
        itemRV.setAdapter(itemAdapter);
    }

    //load the drawer Layout based on user if he logged or guest
    private void changesDrawerLayout() {
        //check if user logged, if so load the logged drawer layout
        if(mAuth.getCurrentUser() != null){
            //this (if) to remove nav header if its already existed
            if(navigationView.getHeaderCount() != 0) {
                navigationView.removeHeaderView(headerView);
            }
            //add nav header
            navigationView.addHeaderView(headerView);
            //get user info from DB and assign it to header views
            final TextView headerNameTV = headerView.findViewById(R.id.nav_header_name_tv);
            final TextView headerEmailTV = headerView.findViewById(R.id.nav_header_email_tv);
            final CircleImageView headerIV = headerView.findViewById(R.id.nav_header_iv);
            firestore.collection("Users").document(Objects.requireNonNull(mAuth.getUid())).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    //assign values to header views
                    headerNameTV.setText(documentSnapshot.getString("name"));
                    headerEmailTV.setText(documentSnapshot.getString("email"));
                    String profileImgUrl = documentSnapshot.getString("img_url");
                    assert profileImgUrl != null;
                    if (!profileImgUrl.equals("default")) {
                        RequestOptions requestOptions = new RequestOptions().placeholder(R.drawable.user_profile);
                        Glide.with(MainActivity.this).setDefaultRequestOptions(requestOptions).load(profileImgUrl).into(headerIV);
                    }else{
                        Glide.with(MainActivity.this).load(R.drawable.user_profile).into(headerIV);
                    }
                }
            });
            //load the drawer layout of logged user
            navigationView.getMenu().clear();
            navigationView.inflateMenu(R.menu.navigation_drawer_logged_users);

            //set up the chat badge if there is a new messages
            setUpBadge();
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
    public void populateRV() {
        itemList.clear();//to avoid repetition

        //show progress dialog
        pd = new ProgressDialog(this);
        pd.setMessage("Loading");
        pd.show();

        //show in the filter option in the toolbar
        getSupportActionBar().setSubtitle( "Category:"+ selectedCategory+ ", Condition:" + selectedCondition);

        //send query to firestore DEPENDING of the filters, add the items them to recyclerview.
        if(selectedCategory.equalsIgnoreCase("All") &&
                selectedCondition.equalsIgnoreCase("All")) {
            //if both category and condition are All
            firestore.collection("Items").orderBy("timePosted", Query.Direction.DESCENDING).get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                    if (queryDocumentSnapshots != null) {
                        //get all items, add them to itemList
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            ItemModel item = doc.toObject(ItemModel.class);
                            itemList.add(item);
                        }
                        //notify adapter
                        itemAdapter = new ItemAdapter(itemList);
                        itemRV.setAdapter(itemAdapter);

                        pd.dismiss();
                    }
                }
            });
        }else if(!selectedCategory.equalsIgnoreCase("All") &&
                selectedCondition.equalsIgnoreCase("All")){
            //if category is not All
            firestore.collection("Items").whereEqualTo("category", selectedCategory)
                    .orderBy("timePosted", Query.Direction.DESCENDING).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                    if (queryDocumentSnapshots != null) {
                        //get all items, add them to itemList
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            ItemModel item = doc.toObject(ItemModel.class);
                            itemList.add(item);
                        }
                        //notify adapter
                        itemAdapter = new ItemAdapter(itemList);
                        itemRV.setAdapter(itemAdapter);

                        pd.dismiss();
                    }
                }
            });

        }else if(selectedCategory.equalsIgnoreCase("All") &&
                !selectedCondition.equalsIgnoreCase("All")){
            //if condition is not All
            firestore.collection("Items").whereEqualTo("condition", selectedCondition)
                    .orderBy("timePosted", Query.Direction.DESCENDING).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                    if (queryDocumentSnapshots != null) {
                        //get all items, add them to itemList
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            ItemModel item = doc.toObject(ItemModel.class);
                            itemList.add(item);
                        }
                        //notify adapter
                        itemAdapter = new ItemAdapter(itemList);
                        itemRV.setAdapter(itemAdapter);

                        pd.dismiss();
                    }
                }
            });

        }else{
            //if both category and condition are not All
            firestore.collection("Items").whereEqualTo("category", selectedCategory)
                    .whereEqualTo("condition", selectedCondition).orderBy("timePosted", Query.Direction.DESCENDING).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                    if (queryDocumentSnapshots != null) {
                        //get all items, add them to itemList
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            ItemModel item = doc.toObject(ItemModel.class);
                            itemList.add(item);
                        }
                        //notify adapter
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
    //setup and display bottomSheet
    private void filterBottomSheet() {
        //inflate (filter_bottom_layout) and its views for bottom sheet
        View view = LayoutInflater.from(this).inflate(R.layout.filter_bottom_layout, null);
        Spinner categorySpinner = view.findViewById(R.id.bottom_sheet_category_spinner);
        Spinner conditionSpinner = view.findViewById(R.id.bottom_sheet_condition_spinner);
        Button applyBtn = view.findViewById(R.id.bottom_sheet_apply_btn);

        //spinner's values
        final String[] categories = getResources().getStringArray(R.array.spinner_category_search);
        final String[] conditions = getResources().getStringArray(R.array.spinner_condition_search);

        //create an array adapter using the string array and default spinner layout
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, conditions);
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
                selectedCategory = categories[position];
                selectedCategoryPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        conditionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCondition = conditions[position];
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

    //check if there is new msgs by sending query to Chatlist--->newMsgs, if so show badge in chat item
    private void setUpBadge(){
        //get the badge
        final TextView badge = (TextView)navigationView.getMenu().findItem(R.id.nav_log_chats).getActionView();
        //run query, set visibility of the badge if there is new msgs
        firestore.collection("Chatlist").document(Objects.requireNonNull(mAuth.getUid())).collection("Contacted").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if(queryDocumentSnapshots!=null) {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        if(doc.getBoolean("newMsgs") !=null && doc.getBoolean("newMsgs")){
                            newMsgs = true;
                            break;
                        }
                    }

                    //if there is new msgs show the badge and change the icon of chats
                    if(newMsgs){
                        badge.setVisibility(View.VISIBLE);
                        navigationView.getMenu().findItem(R.id.nav_log_chats).setIcon(R.drawable.ic_message_red);
                    }else{
                        badge.setVisibility(View.GONE);
                        navigationView.getMenu().findItem(R.id.nav_log_chats).setIcon(R.drawable.ic_message);
                    }
                }
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

        //if changes happen in another activity, it will refresh the activity
        if(refreshMainActivity){
            refreshActivity();
            refreshMainActivity = false;
        }

        //update the notification token
        FirebaseUser user = mAuth.getCurrentUser();
        if(user !=null){
            //save uid of currently signed in user in shared preference, it will be retreived in FirebaseMessaging
            SharedPreferences sp = getSharedPreferences("SP_USER",MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("Current_USERID",user.getUid());
            editor.apply();
            //update token
            updateToken(FirebaseInstanceId.getInstance().getToken());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        //update the notification token
        FirebaseUser user = mAuth.getCurrentUser();
        if(user !=null){
            //save uid of currently signed in user in shared preference
            SharedPreferences sp = getSharedPreferences("SP_USER",MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("Current_USERID",user.getUid());
            editor.apply();
            //update token
            updateToken(FirebaseInstanceId.getInstance().getToken());// take a look at the this method. its depracted.
            //let's try to change it to new one
        }
    }

    public void updateToken(String token){
        Token mToken = new Token(token);
        firestore.collection("Tokens").document(mAuth.getUid()).set(mToken);
    }

    //Option menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_option_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

         if(item.getItemId() == R.id.option_notification){
            //Go to notification settings
            sendToNotificationSettings();
        }

        return super.onOptionsItemSelected(item);
    }

    //send to the notification settings, to en/disable notification
    private void sendToNotificationSettings(){
        Intent intent = new Intent();
        //add extras to intent depending on your android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", getPackageName());
            intent.putExtra("app_uid", getApplicationInfo().uid);
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + getPackageName()));
        }
        startActivity(intent);
    }

    //method to refresh the activity
    private void refreshActivity(){
        searchET.clearFocus();
        searchET.setText("");
        selectedCategoryPosition = 0;
        selectedConditionPosition = 0;
        selectedCategory = "All";
        selectedCondition = "All";
        populateRV();
        changesDrawerLayout();
    }
}
