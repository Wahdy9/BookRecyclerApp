package com.example.bookrecycler;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.bookrecycler.adapters.MessageAdapter;
import com.example.bookrecycler.models.MessageModel;
import com.example.bookrecycler.models.UserModel;
import com.example.bookrecycler.notification.Data;
import com.example.bookrecycler.notification.Sender;
import com.example.bookrecycler.notification.Token;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

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

    //received from previous activity or notification, used to load info of that user + msgs
    String userId;

    //Recyclerview
    private RecyclerView msgRV;
    private MessageAdapter msgAdapter;
    private List<MessageModel> msgList;

    //location request code constant
    public static final int LOCATION_REQUEST_CODE = 1;

    //volley request queue for notification
    private RequestQueue requestQueue;

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
        pd = new ProgressDialog(MessageActivity.this);

        //initialize firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        //init volley
        requestQueue = Volley.newRequestQueue(getApplicationContext());

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
                        RequestOptions requestOptions = new RequestOptions().placeholder(R.drawable.user_profile);
                        Glide.with(getApplicationContext()).setDefaultRequestOptions(requestOptions).load(user.getImg_url()).into(profile_image);
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MessageActivity.this, "Something went wrong\n  please try again later" , Toast.LENGTH_LONG).show();
                Log.d("MessageActivity", "onFailure(Loading reciever info): " +  e.getMessage());
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

                //check Internet
                if(!Utils.isConnectedToInternet(MessageActivity.this)){
                    Toast.makeText(MessageActivity.this, "Check your Internet connection", Toast.LENGTH_SHORT).show();
                    return;
                }

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
                //check if location permission is granted
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(
                            MessageActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_REQUEST_CODE
                    );
                }else{

                    //check Internet
                    if(!Utils.isConnectedToInternet(MessageActivity.this)){
                        Toast.makeText(MessageActivity.this, "Check your Internet connection", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    getCurrentLocation();
                }
            }
        });


    }

    //this method get the current location(Latitude and longitude) and call sendLocation()
    private void getCurrentLocation() {
        //check if GPS on
        if(locationEnabled()){
            //gps is on, proceed to send the message
            pd.setMessage("Sending Location...");
            pd.setCancelable(false);
            pd.show();

            //setup location request
            final LocationRequest locationRequest = new LocationRequest();
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(3000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            //send location request
            LocationServices.getFusedLocationProviderClient(this)
                    .requestLocationUpdates(locationRequest, new LocationCallback(){
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    LocationServices.getFusedLocationProviderClient(MessageActivity.this).
                            removeLocationUpdates(this);

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
    private void sendLocation(final String sender, final String receiver, double latitude, double longitude) {
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
        firestore.collection("Chats").document(msgId).set(msgMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                pd.dismiss();
                firestore.collection("Users").document(sender).get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        String username = ""+ documentSnapshot.getString("name");
                        //send notification
                        sendNotification(receiver, username, "My Location");

                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
            }
        });

        //upload info about sender and reciever to DB-> Chatlist, so these users will appear in chatListActivity
        Map<String, Object> senderMap = new HashMap<>();
        senderMap.put("id", receiver);
        senderMap.put("newMsgs",false);
        senderMap.put("timestamp", new Timestamp(new Date()));
        firestore.collection("Chatlist").document(sender).collection("Contacted")
                .document(receiver).set(senderMap);

        Map<String, Object> receiverMap = new HashMap<>();
        receiverMap.put("id", sender);
        receiverMap.put("newMsgs",true);//so when reciever logged in the badge will show.
        receiverMap.put("timestamp", new Timestamp(new Date()));
        firestore.collection("Chatlist").document(receiver).collection("Contacted")
                .document(sender).set(receiverMap);
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
    private void sendMessage(final String sender, final String receiver, final String message) {

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
        firestore.collection("Chats").document(msgId).set(msgMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                firestore.collection("Users").document(sender).get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        String username = ""+ documentSnapshot.getString("name");
                        //send notification
                        sendNotification(receiver, username, message);

                    }
                });
            }
        });

        //upload info about sender and reciever to DB-> Chatlist, so these users will appear in chatListActivity
        Map<String, Object> senderMap = new HashMap<>();
        senderMap.put("id", receiver);
        senderMap.put("newMsgs",false);
        senderMap.put("timestamp", new Timestamp(new Date()));
        firestore.collection("Chatlist").document(sender).collection("Contacted")
                .document(receiver).set(senderMap);

        Map<String, Object> receiverMap = new HashMap<>();
        receiverMap.put("id", sender);
        receiverMap.put("newMsgs",true);//so when reciever logged in the badge will show.
        receiverMap.put("timestamp", new Timestamp(new Date()));
        firestore.collection("Chatlist").document(receiver).collection("Contacted"
        ).document(sender).set(receiverMap);
    }

    //load msgs from firestore
    public void readMessages() {
        //show progress dialog
        pd.setMessage("Loading");
        pd.show();

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
                pd.dismiss();
            }
        });
    }

    //method to send notification
    private void sendNotification(final String receiver, final String username, final String message) {
        firestore.collection("Tokens").document(receiver).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                if (!documentSnapshot.exists()){
                    return;
                }

                Token token = documentSnapshot.toObject(Token.class);
                Data data = new Data(""+mAuth.getUid(), ""+username +": "+message, "New Message",
                        ""+receiver,"ChatNotification" ,R.drawable.book_recycler_logo);

                Sender sender = new Sender(data, token.getToken());

                //send the notification to cloud messaging, fcm json object request
                try {
                    JSONObject senderJsonObj = new JSONObject(new Gson().toJson(sender));
                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send",
                            senderJsonObj, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            //response of the request
                            Log.d("JSON_RESPONSE", "onResponse: " + response.toString());

                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("JSON_RESPONSE", "onResponse: " + error.getMessage());
                        }
                    }){
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            //put required headers
                            Map<String,String> headers = new HashMap<>();
                            headers.put("Content-Type", "application/json");
                            headers.put("Authorization", "key=AAAAG3v1T8Q:APA91bFj-5lXId6n3dXd85fSwJB0F-WmkPDIfLZ230mWKGQk8dJvD4EEsTD3s8nzYb2PiA6KyRwboqfMda9-j_rt9BYi_DqWuSan1BttbgrGB1e-InV4d-5_IlOugzjBtNJ948NMwxMn");

                            return headers;
                        }
                    };

                    //add request to queue
                    requestQueue.add(jsonObjectRequest);

                } catch (JSONException e) {
                    e.printStackTrace();
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
