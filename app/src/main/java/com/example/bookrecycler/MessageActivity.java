package com.example.bookrecycler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageActivity extends AppCompatActivity {

    //views
    private CircleImageView profile_image;
    private TextView usernameTV;
    private ImageButton sendBtn, gpsBtn;
    private EditText msgET;

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    //received from previous activity, used to load info of that user + msgs
    String userId;

    //Recyclerview
    private RecyclerView msgRV;
    private MessageAdapter msgAdapter;
    private List<MessageModel> msgList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        //toolbar
        Toolbar toolbar = findViewById(R.id.msg_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //initialize views
        profile_image = findViewById(R.id.msg_profile_image);
        usernameTV = findViewById(R.id.msg_username);
        sendBtn = findViewById(R.id.msg_btn_send);
        msgET = findViewById(R.id.msg_text_send);
        gpsBtn = findViewById(R.id.gps_btn_send);

        //initialize firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        //get reciver's Id (userId) from the intent
        userId = getIntent().getStringExtra("userId");

        //load receiver info(username, image) from firestore
        firestore.collection("Users").document(userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot != null) {
                    UserModel user = documentSnapshot.toObject(UserModel.class);
                    usernameTV.setText(user.getName());
                    if (user.getImg_url().equals("default")) {
                        profile_image.setImageResource(R.drawable.user_profile);
                    } else {
                        RequestOptions requestOptions = new RequestOptions();
                        requestOptions.placeholder(R.drawable.user_profile);
                        Glide.with(getApplicationContext()).setDefaultRequestOptions(requestOptions).load(user.getImg_url()).into(profile_image);
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MessageActivity.this, "error:" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        //initialize recyclerview
        initializeRV();


        //send msg when btn send is clicked
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = msgET.getText().toString();
                if (!msg.equals("")) {
                    sendMessage(mAuth.getUid(), userId, msg);
                } else {
                    Toast.makeText(MessageActivity.this, "You can't send empty message", Toast.LENGTH_SHORT).show();
                }
                msgET.setText("");
            }
        });


    }

    //upload the msg to firestore and add entries in chatList if not exist
    private void sendMessage(String sender, String receiver, String message) {

        HashMap<String, Object> msgMap = new HashMap<>();
        msgMap.put("sender", sender);
        msgMap.put("receiver", receiver);
        msgMap.put("imageUrl", "");
        msgMap.put("isMap", false);
        msgMap.put("isImage", false);
        msgMap.put("message", message);
        msgMap.put("timestamp", FieldValue.serverTimestamp());
        msgMap.put("geoPoint", new GeoPoint(0,0));

        //upload to firestore to Chats
        firestore.collection("Chats").document().set(msgMap);

        //add info about sender and reciever to DB-> Chatlist
        Map<String, Object> senderMap = new HashMap<>();
        senderMap.put("id", receiver);
        senderMap.put("newMsgs",false);
        senderMap.put("timestamp", new Timestamp(new Date()));
        firestore.collection("Chatlist").document(sender).collection("Contacted").document(receiver).set(senderMap);

        Map<String, Object> receiverMap = new HashMap<>();
        receiverMap.put("id", sender);
        receiverMap.put("newMsgs",true);//so when reciever logged in the badge will show.
        receiverMap.put("timestamp", new Timestamp(new Date()));
        firestore.collection("Chatlist").document(receiver).collection("Contacted").document(sender).set(receiverMap);
    }

    //load msgs from firestore
    private void readMessages() {

        msgList.clear();

        firestore.collection("Chats").orderBy("timestamp", Query.Direction.ASCENDING).addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null) {
                    for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {
                        if (doc.getType() == DocumentChange.Type.ADDED) {
                            MessageModel msg = doc.getDocument().toObject(MessageModel.class);
                            if (msg.getReceiver().equals(mAuth.getUid()) && msg.getSender().equals(userId) ||
                                    msg.getReceiver().equals(userId) && msg.getSender().equals(mAuth.getUid())){
                                msgList.add(msg);
                            }
                        }
                    }
                    //msgAdapter.notifyDataSetChanged();//change it if scrolling didnt work
                    msgAdapter = new MessageAdapter(MessageActivity.this, msgList);
                    msgRV.setAdapter(msgAdapter);
                }
            }
        });
    }

    private void initializeRV() {
        msgList = new ArrayList<>();
        msgRV = findViewById(R.id.msgRV);
        msgRV.setHasFixedSize(true);
        msgRV.setNestedScrollingEnabled(false);
    }


    @Override
    protected void onResume() {
        super.onResume();
        readMessages();
    }
}
