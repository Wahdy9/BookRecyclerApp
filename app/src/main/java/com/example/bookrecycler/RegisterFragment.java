package com.example.bookrecycler;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class RegisterFragment extends Fragment {

    //views
    private TextInputLayout emailET, usernameET, phoneET, passwordET, password2ET;
    private Button registerBtn;
    private ProgressBar progressBar;

    //Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    public RegisterFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_register, container, false);

        //initialize views
        emailET = view.findViewById(R.id.reg_email_et);
        usernameET = view.findViewById(R.id.reg_username_et);
        phoneET = view.findViewById(R.id.reg_phone_et);
        passwordET = view.findViewById(R.id.reg_pass_et);
        password2ET = view.findViewById(R.id.reg_pass_et2);
        registerBtn = view.findViewById(R.id.reg_btn);
        progressBar = view.findViewById(R.id.reg_pr);

        //initialize firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        //handle clicks of lign btn
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //check if data is entered correctly
                if (!validateEmail() | !validateUsername() | !validatePassword() | !validatePhone()) {
                    return;
                }

                //get data
                final String name = usernameET.getEditText().getText().toString();
                final String email = emailET.getEditText().getText().toString();
                final String phone = phoneET.getEditText().getText().toString().trim();
                String pass = passwordET.getEditText().getText().toString();

                //display progress bar
                progressBar.setVisibility(View.VISIBLE);
                getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);//to disable user interaction

                //create the user in the database
                mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //account created, upload data to firestore
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("id", mAuth.getCurrentUser().getUid());
                            userMap.put("name", name);
                            userMap.put("email", email);
                            userMap.put("phone", phone);
                            userMap.put("showPhone", true);
                            userMap.put("showEmail", true);
                            userMap.put("img_url", "default");
                            firestore.collection("Users").document(mAuth.getCurrentUser().getUid()).set(userMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //data uploaded successfully to firestore
                                    Toast.makeText(getActivity(), "Your Account Created Successfully", Toast.LENGTH_LONG).show();
                                    //hide progress bar
                                    progressBar.setVisibility(View.GONE);
                                    getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                    getActivity().finish();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    //error when uploading data to firestore
                                    Toast.makeText(getActivity(), "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    //hide progress bar
                                    progressBar.setVisibility(View.GONE);
                                    getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                }
                            });


                        } else {
                            //error with account creation
                            Toast.makeText(getActivity(), "ERROR: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            //hide progress bar
                            progressBar.setVisibility(View.GONE);
                            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        }
                    }
                });


            }
        });

        return view;
    }



    //Validation methods
    boolean validateEmail() {
        String email = emailET.getEditText().getText().toString().trim();

        if (email.isEmpty()) {
            emailET.setError("Field can not be empty!");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailET.setError("Not a valid email address!");
            return false;
        } else {
            emailET.setError(null);
            // emailET.setErrorEnabled(false);//to remove the space for error
            return true;
        }
    }

    boolean validateUsername() {
        String username = usernameET.getEditText().getText().toString().trim();

        if (username.isEmpty()) {
            usernameET.setError("Field can not be empty!");
            return false;
        } else if (username.length() > 15) {
            usernameET.setError("Username too long");
            return false;
        } else {
            usernameET.setError(null);
            return true;
        }
    }

    boolean validatePhone() {
        String phone = phoneET.getEditText().getText().toString().trim();

        if (phone.isEmpty()) {
            usernameET.setError("Field can not be empty!");
            return false;
        } else {
            usernameET.setError(null);
            return true;
        }
    }

    boolean validatePassword() {
        String password = passwordET.getEditText().getText().toString().trim();
        String confirmPass = password2ET.getEditText().getText().toString().trim();

        if (password.isEmpty()) {
            passwordET.setError("Field can not be empty!");
            return false;
        } else if (confirmPass.isEmpty()) {
            password2ET.setError("Field can not be empty!");
            return false;
            /*else if (!PASSWORD_PATTERN.matcher(password).matches()) {
            passwordET.setError("Password too weak");
            return false;
            }*/
        } else if (!password.equals(confirmPass)) {
            password2ET.setError("Passwords doesn't match");
            return false;
        } else {
            passwordET.setError(null);
            return true;
        }
    }


}
