package com.example.bookrecycler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import id.zelory.compressor.Compressor;

public class AddItemActivity extends AppCompatActivity {

    //views
    private EditText titleET, descET, priceET;
    private Spinner categorySpinner, conditionSpinner;
    private Button  addItemBtn;
    private ImageView addImgBtn;

    //firebase
    private FirebaseFirestore firestore;
    private FirebaseStorage firebaseStorage;

    //img picked uri
    private Uri image_uri = null;

    //boolean to determine if its edit mode or not
    private boolean isEditMode;
    //item to edit
    private ItemModel itemToEdit;

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


        //check if its edit mode
        isEditMode = getIntent().getBooleanExtra("edit_mode", false);
        if(isEditMode){
            //change the toolbar title
            getSupportActionBar().setTitle("Edit Item");
            //get the item to edit
            itemToEdit = (ItemModel)getIntent().getSerializableExtra("item_to_edit");

            //load data to the views so we can edit them
            image_uri = Uri.parse(itemToEdit.getItemImg());
            Glide.with(this).load(image_uri).into(addImgBtn);
            titleET.setText(itemToEdit.getTitle());
            priceET.setText(itemToEdit.getPrice());
            descET.setText(itemToEdit.getDesc());
            addItemBtn.setText("Update Item");

            //load the spinners
            int categoryIndex = getCategorySpinnerPosition();
            int conditionIndex = getConditionSpinnerPosition();
            if(categoryIndex != -1){
                categorySpinner.setSelection(categoryIndex);
            }
            if(conditionIndex != -1){
                conditionSpinner.setSelection(conditionIndex);
            }

        }

        //Click listener to handle clicking on the add image
        addImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //*if user running +marshmello, permission is required!
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    //check if permission is not granted, if so ask for it, otherwise start cropping img
                    if(ContextCompat.checkSelfPermission(AddItemActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE )!= PackageManager.PERMISSION_GRANTED){ //no need for write permission, cus we aint writing
                        //it has request code, so if you want to do something with the result, override onRequestPermissionResult(...)
                        ActivityCompat.requestPermissions(AddItemActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    }else{
                        // start picker, check the result in onActivityResult(...)
                        pickImage();
                    }
                }else{
                    pickImage();
                }
            }
        });

        //Click listener to handle clicking on the upload btn
        addItemBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isEditMode){
                    uploadEditedItem();
                }else{
                    uploadItem();
                }
            }
        });

    }



    //return the index of a specific category
    private int getCategorySpinnerPosition() {
        String[] categories = getResources().getStringArray(R.array.spinner_category_types);
        for (int i = 0; i < categories.length;i++) {
            if(categories[i].equalsIgnoreCase(itemToEdit.getCategory())){
                return i;
            }
        }
        return -1;
    }
    //return the index of a specific condition
    private int getConditionSpinnerPosition() {
        String[] conditions = getResources().getStringArray(R.array.spinner_condition_types);
        for (int i = 0; i < conditions.length;i++) {
            if(conditions[i].equalsIgnoreCase(itemToEdit.getCategory())){
                return i;
            }
        }
        return -1;
    }

    //upload the edited item
    private void uploadEditedItem() {
        //get the values
        final String title = titleET.getText().toString().trim();
        final String price = priceET.getText().toString().trim();
        final String description = descET.getText().toString().trim();


        //check if values not empty
        if(!TextUtils.isEmpty(title) && !TextUtils.isEmpty(price) && !TextUtils.isEmpty(description)){
            final ProgressDialog pd = new ProgressDialog(this);
            pd.setMessage("Updating");
            pd.show();

            //check if user picked newer image
            if(!image_uri.toString().equalsIgnoreCase(itemToEdit.getItemImg())){
                //upload the new image with the same name as prevous to overwrite

                //TODO: Compress image here(DONE)
                File imgFile = new File(image_uri.getPath());
                try {
                    Bitmap compressedImg = new Compressor(AddItemActivity.this)
                            .setMaxHeight(400)
                            .setMaxWidth(400)
                            .setQuality(65)
                            .compressToBitmap(imgFile);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    compressedImg.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] imgBytes = baos.toByteArray();

                    //upload img to storage
                    final StorageReference imgStorageRef = firebaseStorage.getReference().child("Items Image").child(itemToEdit.getItemId());
                    imgStorageRef.putBytes(imgBytes).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                //img uploaded successfully
                                //get the download uri
                                imgStorageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {

                                        //create item map to upload it to firestore
                                        final Map<String, Object> itemMap = new HashMap<>();
                                        itemMap.put("title", title);
                                        itemMap.put("price", price);
                                        itemMap.put("desc", description);
                                        itemMap.put("category", categorySpinner.getSelectedItem().toString());
                                        itemMap.put("condition", conditionSpinner.getSelectedItem().toString());
                                        itemMap.put("itemImg", uri.toString());

                                        //upload to firestroe, update the document
                                        firestore.collection("Items").document(itemToEdit.getItemId()).update(itemMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(AddItemActivity.this, "Item updated successfully", Toast.LENGTH_LONG).show();
                                                    MainActivity.refreshMainActivity = true;//to refresh the MainActivity
                                                    MyItemsActivity.refreshMyItemsActivity = true;//to refresh the MyItemActivity
                                                    finish();
                                                } else {
                                                    Toast.makeText(AddItemActivity.this, "Firestore error:" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                }
                                                pd.dismiss();
                                            }
                                        });

                                    }
                                });
                            }
                        }
                    });
                }catch (IOException e) {
                    //compress image exception
                    pd.dismiss();
                    Toast.makeText(AddItemActivity.this, "error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }



            }else{
                //if user didn't pick a new image, just update the other fields
                //create item map to upload it to firestore
                final Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("title", title);
                itemMap.put("price", price);
                itemMap.put("desc", description);
                itemMap.put("category", categorySpinner.getSelectedItem().toString());
                itemMap.put("condition", conditionSpinner.getSelectedItem().toString());

                //upload to firestroe, update the document
                firestore.collection("Items").document(itemToEdit.getItemId()).update(itemMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(AddItemActivity.this, "Item updated successfully", Toast.LENGTH_LONG).show();
                            MainActivity.refreshMainActivity = true;//to refresh the MainActivity
                            MyItemsActivity.refreshMyItemsActivity = true;//to refresh the MyItemActivity
                            finish();
                        } else {
                            Toast.makeText(AddItemActivity.this, "Firestore error:" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                        pd.dismiss();
                    }
                });
            }


        }else{
            //fields values not filled
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
        }

    }

    //upload the item
    private void uploadItem() {
        //get the values
        final String title = titleET.getText().toString().trim();
        final String price = priceET.getText().toString().trim();
        final String description = descET.getText().toString().trim();

        //check if values not empty
        if(!TextUtils.isEmpty(title) && !TextUtils.isEmpty(price) && !TextUtils.isEmpty(description)){
            if(image_uri != null) {
                final ProgressDialog pd = new ProgressDialog(this);
                pd.setMessage("Uploading");
                pd.show();

                //get random itemID
                final DocumentReference itemRef = firestore.collection("Items").document();
                final String itemId = itemRef.getId();

                //TODO: Compress image here(DONE)
                File imgFile = new File(image_uri.getPath());
                try {
                    Bitmap compressedImg = new Compressor(AddItemActivity.this)
                            .setMaxHeight(400)
                            .setMaxWidth(400)
                            .setQuality(65)
                            .compressToBitmap(imgFile);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    compressedImg.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] imgBytes = baos.toByteArray();


                    //upload img to storage
                    final StorageReference imgStorageRef = firebaseStorage.getReference().child("Items Image").child(itemId);
                    imgStorageRef.putBytes(imgBytes).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                //img uploaded successfully
                                //get the download uri
                                imgStorageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {

                                        //create item map to upload it to firestore
                                        final Map<String, Object> itemMap = new HashMap<>();
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
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(AddItemActivity.this, "Item uploaded successfully", Toast.LENGTH_LONG).show();
                                                    MainActivity.refreshMainActivity = true;//to refresh the MainActivity
                                                    MyItemsActivity.refreshMyItemsActivity = true;//to refresh the MyItemActivity
                                                    finish();
                                                } else {
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

                }catch (IOException e) {
                    //compress image exception
                    pd.dismiss();
                    Toast.makeText(AddItemActivity.this, "error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }else{
                //image is not picked
                Toast.makeText(this, "Please pick an image", Toast.LENGTH_SHORT).show();
            }

        }else{
            //fields values not filled
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
        }

    }



    //Pick an image using cropper
    private void pickImage() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(AddItemActivity.this);
    }


    //permission results received here
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode ==1) {
            if (grantResults.length > 0) {
                boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (storageAccepted) {
                    pickImage();
                } else {
                    Toast.makeText(this, "Storage permission is required!", Toast.LENGTH_SHORT).show();
                }
            }

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    //results of picking an image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                image_uri = result.getUri();
                addImgBtn.setImageURI(image_uri);//set the profile img with img picked.


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(AddItemActivity.this, "error: " + result.getError(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
