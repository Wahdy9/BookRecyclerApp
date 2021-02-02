package com.example.bookrecycler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;

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

    //Recyclerview
    private RecyclerView chatsRV;
    private ChatListAdapter chatsAdapter;

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    private List<UserModel> mUsers;//list of users obj in DB that we chatted with,, we get them from Users
    private List<ChatListModel> usersList;//hold usersID's that we chatted with,, we get them from Chatlist

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

        //initialize recyclerview
        initializeRV();

        //initialize firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();


        //get users we chatted with, display them in RV
        firestore.collection("Chatlist").document(mAuth.getUid()).collection("Contacted").orderBy("timestamp", Query.Direction.ASCENDING).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if(queryDocumentSnapshots != null){
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        ChatListModel chatlist= doc.toObject(ChatListModel.class);
                        usersList.add(chatlist);
                    }
                    loadChatList();
                }
            }
        });

    }


    //method to compare the ID's in Chatlist with all users, and create user obj if we contacted that user
    private void loadChatList() {

        firestore.collection("Users").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if(queryDocumentSnapshots != null){
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        UserModel user= doc.toObject(UserModel.class);
                        for (ChatListModel chatlist : usersList){
                            if (user.getId().equals(chatlist.getId())){
                                mUsers.add(user);
                            }
                        }

                    }
                    chatsAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void initializeRV() {
        mUsers = new ArrayList<>();
        usersList = new ArrayList<>();

        chatsRV = findViewById(R.id.chat_list_rv);
        chatsRV.setHasFixedSize(true);
        chatsAdapter = new ChatListAdapter(this, mUsers);
        chatsRV.setAdapter(chatsAdapter);
    }
}
