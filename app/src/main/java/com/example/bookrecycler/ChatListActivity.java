package com.example.bookrecycler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {


    //Views
    private EditText searchET;

    //Recyclerview
    private RecyclerView chatsRV;
    private ChatListAdapter chatsAdapter;

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    private List<UserModel> mUsers;//list of users obj in DB that we chatted with,, we get them from Users
    private List<String> myContactsIds;//list of user Ids that we chatted with,, we get them from Chatlists

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        //toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("My Chats");
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //initialize views
        searchET = findViewById(R.id.chatlist_search_et);

        //initialize recyclerview
        initializeRV();

        //initialize firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

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

        //get users we chatted with, display them in RV
        firestore.collection("Chatlist").document(mAuth.getUid()).collection("Contacted").orderBy("timestamp", Query.Direction.ASCENDING).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if(queryDocumentSnapshots != null){
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        myContactsIds.add(doc.getId());
                    }
                    loadChatList();
                }
            }
        });

    }


    //method to create user obj by sending query with IDs in myContactsIds list
    private void loadChatList() {

        firestore.collection("Users").whereIn("id", myContactsIds).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if(queryDocumentSnapshots != null){
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        UserModel user= doc.toObject(UserModel.class);
                        mUsers.add(user);
                    }
                    chatsAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    //this method used to filter RV when typing in search field
    private void filter(String text) {
        ArrayList<UserModel> filteredList = new ArrayList<>();

        for (int i = 0; i < mUsers.size(); i++) {
            if (mUsers.get(i).getName().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(mUsers.get(i));
            }
        }
        chatsAdapter = new ChatListAdapter(this, filteredList);
        chatsRV.setAdapter(chatsAdapter);
    }

    private void initializeRV() {
        mUsers = new ArrayList<>();
        myContactsIds =  new ArrayList<>();

        chatsRV = findViewById(R.id.chat_list_rv);
        chatsRV.setHasFixedSize(true);
        chatsAdapter = new ChatListAdapter(this, mUsers);
        chatsRV.setAdapter(chatsAdapter);
    }
}
