package com.example.bookrecycler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
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
    private ProgressDialog pd;

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    public ListenerRegistration registration;//for firestore snapshat listner, so we can deattatch it from adapter to refresh RV

    //received from previous activity, used to load info of that user + msgs
    String userId;

    //Recyclerview
    private RecyclerView msgRV;
    private MessageAdapter msgAdapter;
    private List<MessageModel> msgList;

    //location request code constant
    public static final int LOCATION_REQUEST_CODE = 1;


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


        //go to UserProfileActivity when clicking the username
        usernameTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MessageActivity.this, UsersProfileActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

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

        //send location when GPS btn  is clicked
        gpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(
                            MessageActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_REQUEST_CODE
                    );
                }else{
                    getCurrentLocation();
                }
            }
        });


    }

    //this method get the current location(Latitude and longitutde) and call sendlocation()
    private void getCurrentLocation() {
        //check if GPS on
        if(locationEnabled()){
            //gps is on, proceed to send the message
            pd = new ProgressDialog(MessageActivity.this);
            pd.setMessage("Sending Location...");
            pd.show();

            //setup location request
            final LocationRequest locationRequest = new LocationRequest();
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(3000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            //send location request
            LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, new LocationCallback(){
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    LocationServices.getFusedLocationProviderClient(MessageActivity.this).removeLocationUpdates(this);

                    if(locationResult != null && locationResult.getLocations().size() > 0){
                        int latestLocationIndex = locationResult.getLocations().size() -1;

                        double latitude = locationResult.getLocations().get(latestLocationIndex).getLatitude();
                        double longitude = locationResult.getLocations().get(latestLocationIndex).getLongitude();

                        //send the message
                        sendLocation(mAuth.getUid(),userId,latitude, longitude);
                    }
                }
            }, Looper.getMainLooper());
        }else{
            //gos is off, show dialog to send user to the setting to enable it
            new AlertDialog.Builder(MessageActivity.this )
                    .setMessage( "Enable GPS" )
                    .setPositiveButton( "Settings" , new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick (DialogInterface paramDialogInterface , int paramInt) {
                                    startActivity( new Intent(Settings. ACTION_LOCATION_SOURCE_SETTINGS )) ;
                                }
                            })
                    .setNegativeButton( "Cancel" , null )
                    .show() ;
        }

    }

    //upload the location to firestore and add entries in chatList if not exist
    private void sendLocation(String sender, String receiver,double latitude, double longitude) {

        //generate random id for the msg
        final DocumentReference msgRef = firestore.collection("Chats").document();
        final String msgId = msgRef.getId();

        //create msg map
        HashMap<String, Object> msgMap = new HashMap<>();
        msgMap.put("id", msgId);
        msgMap.put("sender", sender);
        msgMap.put("receiver", receiver);
        msgMap.put("imageUrl", "");
        msgMap.put("map", true);
        msgMap.put("image", false);
        msgMap.put("message", "");
        msgMap.put("timestamp", new Timestamp(new Date()));
        msgMap.put("geoPoint", new GeoPoint(latitude,longitude));

        //upload to firestore to Chats
        firestore.collection("Chats").document(msgId).set(msgMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                pd.dismiss();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
            }
        });

        //upload info about sender and reciever to DB-> Chatlist, so these users will aprear in chatListActivity
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

    //this method check if GPS is on or off, return true if on, false otherwise
    private boolean locationEnabled() {
        LocationManager lm = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE ) ;
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager. GPS_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager. NETWORK_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }

        return (gps_enabled && network_enabled);
    }

    //upload the msg to firestore and add entries in chatList if not exist
    private void sendMessage(String sender, String receiver, String message) {

        //generate random id for the msg
        final DocumentReference msgRef = firestore.collection("Chats").document();
        final String msgId = msgRef.getId();

        //create msg map
        HashMap<String, Object> msgMap = new HashMap<>();
        msgMap.put("id", msgId);
        msgMap.put("sender", sender);
        msgMap.put("receiver", receiver);
        msgMap.put("imageUrl", "");
        msgMap.put("map", false);
        msgMap.put("image", false);
        msgMap.put("message", message);
        msgMap.put("timestamp", new Timestamp(new Date()));
        msgMap.put("geoPoint", new GeoPoint(0,0));

        //upload to firestore to Chats
        firestore.collection("Chats").document(msgId).set(msgMap);

        //upload info about sender and reciever to DB-> Chatlist, so these users will aprear in chatListActivity
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
    public void readMessages() {

        msgList.clear();

        //snapshot listener to get the messages
        Query query = firestore.collection("Chats").orderBy("timestamp", Query.Direction.ASCENDING);
        registration = query.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null) {
                    for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {
                        if (doc.getType() == DocumentChange.Type.ADDED) {
                            MessageModel msg = doc.getDocument().toObject(MessageModel.class);
                            //check if msg belong the two users
                            if (msg.getReceiver().equals(mAuth.getUid()) && msg.getSender().equals(userId) ||
                                    msg.getReceiver().equals(userId) && msg.getSender().equals(mAuth.getUid())){
                                msgList.add(msg);
                            }
                        }

                    }
                    msgAdapter = new MessageAdapter(MessageActivity.this, msgList);
                    msgRV.setAdapter(msgAdapter);
                }
            }
        });
    }

    //initialize recycler view
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

    //location permeission request received here, if allow call getCurrentLocation()
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == LOCATION_REQUEST_CODE && grantResults.length >0){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //location permission is granted
                getCurrentLocation();
            }else{
                //location permission is denied
                Toast.makeText(this, "Location permission is required!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
