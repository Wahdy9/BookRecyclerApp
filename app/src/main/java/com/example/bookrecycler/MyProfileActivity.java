package com.example.bookrecycler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class MyProfileActivity extends AppCompatActivity {

    private EditText usernameET, phoneET, emailET,majorET;
    private CircleImageView profileImg;
    private Button saveBtn;
    private Switch phoneSwitch;
    private Switch emailSwitch;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private StorageReference mStorageRef;

    private boolean isChanged;
    private Uri mainImgUri;

    private String downloadUrl = "default";

    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        //Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("My Profile");
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //when click backarrow finish this activity, to avoid refresh the mainActivity
            }
        });

        //initialize views
        usernameET = findViewById(R.id.profile_username_et);
        phoneET = findViewById(R.id.profile_phone_et);
        emailET = findViewById(R.id.profile_email_et);
        majorET = findViewById(R.id.profile_major_et);
        profileImg = findViewById(R.id.profile_img);
        saveBtn = findViewById(R.id.profile_save_btn);
        phoneSwitch = findViewById(R.id.profile_phone_switch);
        emailSwitch = findViewById(R.id.profile_email_switch);
        pd = new ProgressDialog(MyProfileActivity.this);

        //initialize firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        //load data from firestore
        pd.setMessage("Loading");
        pd.show();
        firestore.collection("Users").document(mAuth.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    //get data
                    String username = task.getResult().getString("name");
                    String phone = task.getResult().getString("phone");
                    String email  = task.getResult().getString("email");
                    String profileImgUrl = task.getResult().getString("img_url");
                    boolean showPhone = task.getResult().getBoolean("showPhone");
                    boolean showEmail = task.getResult().getBoolean("showEmail");
                    String major = task.getResult().getString("major");

                    //assign data to views
                    usernameET.setText(username);
                    phoneET.setText(phone);
                    emailET.setText(email);
                    majorET.setText(major);
                    phoneSwitch.setChecked(showPhone);
                    emailSwitch.setChecked(showEmail);
                    if(profileImgUrl != null) {
                        if (!profileImgUrl.equals("default")) {

                            RequestOptions requestOptions = new RequestOptions();
                            requestOptions.placeholder(R.drawable.user_profile);
                            Glide.with(MyProfileActivity.this).setDefaultRequestOptions(requestOptions).load(profileImgUrl).into(profileImg);
                            downloadUrl = profileImgUrl;

                        }
                    }
                    pd.dismiss();
                }else{
                    Toast.makeText(MyProfileActivity.this, "Error:" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    pd.dismiss();
                }
            }
        });

        //ask permissions, then pick img from phone
        profileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //*if user running +marshmello, permission is required!
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                    //check if permission is not granted, if so ask for it, otherwise start cropping img
                    if(ContextCompat.checkSelfPermission(MyProfileActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE )!= PackageManager.PERMISSION_GRANTED){ //no need for write permission, cus we aint writing
                        //it has request code, so if you want to do something with the result, override onRequestPermissionResult(...)
                        ActivityCompat.requestPermissions(MyProfileActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    }else{
                        // start picker, check the result in onActivityResult(...)
                        pickImage();
                    }

                }else{
                    pickImage();
                }
            }
        });

        //upload updated data
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //get data
                String name = usernameET.getText().toString().trim();
                String phone = phoneET.getText().toString().trim();
                String major = majorET.getText().toString().trim();

                //Progress dialog
                pd.setMessage("Uploading");
                pd.show();

                //check if values are empty
                if(!TextUtils.isEmpty(name)  && !TextUtils.isEmpty(phone)){
                    //create user map
                    final Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", mAuth.getCurrentUser().getUid());
                    userMap.put("name" , name);
                    userMap.put("phone", phone);
                    userMap.put("major", major);
                    userMap.put("email", mAuth.getCurrentUser().getEmail());
                    userMap.put("showPhone", phoneSwitch.isChecked());
                    userMap.put("showEmail", emailSwitch.isChecked());

                    if(isChanged){//mean user picked img
                        //compress Image
                        File imgFile = new File(mainImgUri.getPath());
                        try {
                            Bitmap compressedImg =  new Compressor(MyProfileActivity.this)
                                    .setMaxHeight(100)
                                    .setMaxWidth(100)
                                    .setQuality(50)
                                    .compressToBitmap(imgFile);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            compressedImg.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                            byte[] imgBytes = baos.toByteArray();

                            //upload to storage
                            final StorageReference imgStoragePath = mStorageRef.child("Profile Images").child(mAuth.getCurrentUser().getUid());
                            imgStoragePath.putBytes(imgBytes).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                    if(task.isSuccessful()){
                                        //get download url
                                        imgStoragePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                userMap.put("img_url", uri.toString());
                                                //upload to firestore
                                                uploadToFirestore(userMap);
                                            }
                                        });

                                    }else{
                                        pd.dismiss();
                                        Toast.makeText(MyProfileActivity.this, "STORAGE ERROR: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        } catch (IOException e) {
                            //compress image exception
                            pd.dismiss();
                            Toast.makeText(MyProfileActivity.this, "error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }



                    }else{//mean user didn't pick img, uri will be same as previuos image
                        userMap.put("img_url", downloadUrl);
                        uploadToFirestore(userMap);
                    }

                }else{
                    pd.dismiss();
                    Toast.makeText(MyProfileActivity.this, "Name & phone must be filled", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    private void uploadToFirestore(Map userMap){
        //upload everything to firestore
        firestore.collection("Users").document(mAuth.getUid()).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(MyProfileActivity.this, "Profile Updated Successfully!", Toast.LENGTH_LONG).show();

                }else{
                    Toast.makeText(MyProfileActivity.this, "FIRESTROE ERROR: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
                pd.dismiss();
            }
        });
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == -1) {
                mainImgUri = result.getUri();
                profileImg.setImageURI(mainImgUri);//set the profile img with img picked.
                isChanged = true;

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(MyProfileActivity.this, "error: " + result.getError(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    //pick image from gallery or camera
    private void pickImage() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1,1)
                .start(MyProfileActivity.this);

    }
}
