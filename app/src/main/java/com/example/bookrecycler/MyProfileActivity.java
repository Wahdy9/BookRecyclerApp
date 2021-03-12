package com.example.bookrecycler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
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

    //Views
    private EditText usernameET, phoneET, emailET,majorET;
    private CircleImageView profileImg;
    private Button saveBtn;
    private Switch phoneSwitch;
    private Switch emailSwitch;
    private TextView avgRatingTV, changePassTV;

    private TextInputLayout passwordET, password2ET;
    private Button changeBtn, cancelBtn;

    //Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private StorageReference mStorageRef;

    //Profile image variable
    private boolean isChanged;//to check if user picked a new image
    private Uri mainImgUri;//uri of the profile image

    private String downloadUrl = "default";//download url of the uploaded image

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
        avgRatingTV =  findViewById(R.id.profile_avg_rating_tv);
        changePassTV = findViewById(R.id.profile_change_pass);

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
                    //error loading data
                    Toast.makeText(MyProfileActivity.this, "Something went wrong\nplease try again later" , Toast.LENGTH_LONG).show();
                    Log.d("MyProfileActivity", "onComplete(Loading data from firestore): "+ task.getException().getMessage());
                    pd.dismiss();
                }
            }
        });

        //load the avg rating
        firestore.collection("Users").document(mAuth.getUid()).collection("Ratings").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if (queryDocumentSnapshots != null) {
                    double sum = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        sum = sum + (double)doc.get("stars");
                    }

                    int noOfRatings = queryDocumentSnapshots.getDocuments().size();
                    if(noOfRatings==0){
                        //if there is no rating, make it 0/5
                        avgRatingTV.setText("0/5");
                    }else{
                        //if there is rating, get the avg
                        avgRatingTV.setText(String.format("%.1f", (sum/noOfRatings))+ "/5");
                    }
                }
            }
        });

        //pick image when clicking profile image
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

        //change password click
        changePassTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPassChangeDialog();
            }
        });

        //upload updated data, when btn is clicked
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
                    //check if username is not more than 15 character
                    if(name.length() >15){
                        Toast.makeText(MyProfileActivity.this, "Name must not exceed 15 characters", Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                        return;
                    }

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
                                        //image uploaded successfully, get download url
                                        imgStoragePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                userMap.put("img_url", uri.toString());
                                                //upload to firestore
                                                uploadToFirestore(userMap);
                                            }
                                        });

                                    }else{
                                        //image failed to upload
                                        pd.dismiss();
                                        Toast.makeText(MyProfileActivity.this, "Something went wrong\nplease try again later", Toast.LENGTH_LONG).show();
                                        Log.d("MyProfileActivity", "onComplete(Image upload): "+task.getException().getMessage());
                                    }
                                }
                            });
                        } catch (IOException e) {
                            //compress image exception
                            pd.dismiss();
                            Toast.makeText(MyProfileActivity.this, "Something went wrong\nplease try again later", Toast.LENGTH_LONG).show();
                            Log.d("MyProfileActivity", "onClick(Compress image): " + e.getMessage());
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

    //method to change password
    private void showPassChangeDialog() {
        //set up dialog layout
        AlertDialog.Builder builder = new AlertDialog.Builder(MyProfileActivity.this);
        View layoutDialog = LayoutInflater.from(MyProfileActivity.this).inflate(R.layout.change_password_layout, null);
        builder.setView(layoutDialog);

        //init views
        passwordET = layoutDialog.findViewById(R.id.change_pass_et);
        password2ET = layoutDialog.findViewById(R.id.change_pass_et2);
        changeBtn = layoutDialog.findViewById(R.id.change_pass_btn);
        cancelBtn = layoutDialog.findViewById(R.id.cancel_pass_btn);

        //show dialog
        final AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
        dialog.setCancelable(false);
        dialog.getWindow().setGravity(Gravity.CENTER);

        //cancel btn clicked
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        //change btn clicked
        changeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //validate password
                if(!validatePassword()){
                    return;
                }

                //get password
                final String pass = passwordET.getEditText().getText().toString();

                //update password in firebase
                final FirebaseUser user = mAuth.getCurrentUser();
                user.updatePassword(pass).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()) {
                            //password updated
                            dialog.dismiss();
                            Toast.makeText(MyProfileActivity.this, "Password update successfully", Toast.LENGTH_SHORT).show();

                        } else {
                            //password fail to update, show error
                            if(task.getException() instanceof FirebaseAuthRecentLoginRequiredException){
                                //this exception requires recent login in order for change password to work
                                Toast.makeText(MyProfileActivity.this, "You need to logout and login to use this feature", Toast.LENGTH_SHORT).show();
                            }else {
                                Toast.makeText(MyProfileActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                            Log.d("MyProfileActivity", "Error password not updated= " + task.getException().toString());
                        }
                    }
                });

            }
        });

    }

    //method to validate password
    boolean validatePassword() {
        String password = passwordET.getEditText().getText().toString().trim();
        String confirmPass = password2ET.getEditText().getText().toString().trim();

        if (password.isEmpty()) {
            passwordET.setError("Field can not be empty!");
            return false;
        }else if (confirmPass.isEmpty()) {
            password2ET.setError("Field can not be empty!");
            return false;
        } else if (!password.equals(confirmPass)) {
            password2ET.setError("Password doesn't match");
            return false;
        } else {
            passwordET.setError(null);
            return true;
        }
    }

    //method to upload data to firestore
    private void uploadToFirestore(Map userMap){
        //upload everything to firestore
        firestore.collection("Users").document(mAuth.getUid()).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(MyProfileActivity.this, "Profile Updated Successfully!", Toast.LENGTH_LONG).show();
                    MainActivity.refreshMainActivity = true;//to refresh the MainActivity
                }else{
                    Toast.makeText(MyProfileActivity.this, "Something went wrong\n  please try again later", Toast.LENGTH_LONG).show();
                    Log.d("MyProfileActivity", "onComplete(Upload to firestore): " + task.getException().getMessage());
                }
                pd.dismiss();
            }
        });
    }


    //results of picking up image here
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
                Toast.makeText(MyProfileActivity.this, "Error picking up image", Toast.LENGTH_SHORT).show();
                Log.d("MyProfileActivity", "onActivityResult(Pick up image): " + result.getError());
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
