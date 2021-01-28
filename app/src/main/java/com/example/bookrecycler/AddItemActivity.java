package com.example.bookrecycler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AddItemActivity extends AppCompatActivity {


    //views
    private EditText titleET, descET, priceET;
    private Spinner categorySpinner, conditionSpinner;
    private Button  addItemBtn;
    private ImageView addImgBtn;

    //firebase
    private FirebaseFirestore firestore;
    private FirebaseStorage firebaseStorage;

    //permission arries
    private String[] cameraPermission;
    private String[] storagePermission;

    //permission constant
    public static final int CAMERA_REQUEST_CODE = 100;
    public static final int STORAGE_REQUEST_CODE = 200;
    //image pick content
    public static final int IMAGE_PICK_GALLERY_CODE = 300;
    public static final int IMAGE_PICK_CAMERY_CODE = 400;

    //img picked uri
    Uri image_uri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        //Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Add New Item");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        //init views
        titleET = findViewById(R.id.add_item_title);
        priceET = findViewById(R.id.add_item_price);
        descET = findViewById(R.id.add_item_desc);
        categorySpinner = findViewById(R.id.add_item_category_spinner);
        conditionSpinner = findViewById(R.id.add_item_condition_spinner);
        addImgBtn = findViewById(R.id.add_item_img);
        addItemBtn = findViewById(R.id.add_item_btn);

        //init firabase
        firestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        //init permission arraies
        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        addImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickDialog();
            }
        });

        addItemBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadItem();
            }
        });

    }

    private void uploadItem() {
        //get the values
        final String title = titleET.getText().toString().trim();
        final String price = priceET.getText().toString().trim();
        final String description = descET.getText().toString().trim();

        //check if values not empty
        if(!TextUtils.isEmpty(title) && !TextUtils.isEmpty(price) && !TextUtils.isEmpty(description) && image_uri != null){

            final ProgressDialog pd = new ProgressDialog(this);
            pd.setMessage("Uploading");
            pd.show();

            //get random itemID
            final DocumentReference itemRef = firestore.collection("Items").document();
            final String itemId = itemRef.getId();


            //upload img to storage
            final StorageReference imgStorageRef = firebaseStorage.getReference().child("Items Image").child(itemId);
            imgStorageRef.putFile(image_uri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        //img uploaded successfully
                        //get the download uri
                        imgStorageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {

                                //create item map to upload it to firestore
                                final Map<String,Object> itemMap = new HashMap<>();
                                itemMap.put("itemId", itemId);
                                itemMap.put("userId", FirebaseAuth.getInstance().getUid());
                                itemMap.put("title", title);
                                itemMap.put("price", price);
                                itemMap.put("desc", description);
                                itemMap.put("category", categorySpinner.getSelectedItem().toString());
                                itemMap.put("condition", conditionSpinner.getSelectedItem().toString());
                                itemMap.put("itemImg", uri.toString());
                                itemMap.put("timePosted", new Timestamp(new Date()));

                                //upload to firestroe
                                firestore.collection("Items").document(itemId).set(itemMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            Toast.makeText(AddItemActivity.this, "Item uploaded successfully", Toast.LENGTH_LONG).show();
                                            finish();
                                        }else{
                                            Toast.makeText(AddItemActivity.this, "Firestore error:" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                        pd.dismiss();
                                    }
                                });

                            }
                        });


                    } else {
                        pd.dismiss();
                        Toast.makeText(AddItemActivity.this, "Storage Error : " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });



        }else{
            //fields values not filled
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
        }




    }

    //show a dialog to pick the image from gallery or camera
    private void showImagePickDialog() {
        //options to display in dialog
        String[] options = {"Camera", "Gallery"};

        //dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Image")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            //camera, check if permission granted
                            if (checkCameraPermission()) {
                                pickFromCamera();
                            } else {
                                requestCameraPermission();
                            }
                        } else if(which == 1){
                            //gallery, check if permission granted
                            if (checkStoragePermission()) {
                                pickFromGallery();
                            } else {
                                requestStoragePermission();
                            }
                        }
                    }
                }).show();
    }


    //true if permission already granted, false otherwise.
    boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }
    //request permission(show popup) --> result in activityPermissionResult
    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermission,STORAGE_REQUEST_CODE);
    }
    boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 =  ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission,CAMERA_REQUEST_CODE);
    }

    void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    void pickFromCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp Image Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp Image Description");

        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERY_CODE);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted) {
                        pickFromCamera();
                    } else {
                        Toast.makeText(this, "Camera permission is required!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted) {
                        pickFromGallery();
                    } else {
                        Toast.makeText(this, "Storage permission is required!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;

        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                //get picked imgs
                image_uri = data.getData();
                //set to img view
                addImgBtn.setImageURI(image_uri);
            } else if (requestCode == IMAGE_PICK_CAMERY_CODE) {
                addImgBtn.setImageURI(image_uri);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
