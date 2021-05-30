package com.book.bookrecycler;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginFragment extends Fragment {

    //views
    private TextInputLayout emailET, passwordET;
    private Button loginBtn;
    private TextView forgetPassTV;
    private ProgressBar progressBar;

    //Firebase
    private FirebaseAuth mAuth;

    //for google signin
    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;
    private SignInButton googleSigninBtn;


    public LoginFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        //initialize views
        emailET = view.findViewById(R.id.log_email_et);
        passwordET = view.findViewById(R.id.log_pass_et);
        loginBtn = view.findViewById(R.id.log_btn);
        forgetPassTV = view.findViewById(R.id.log_forgot_password);
        progressBar = view.findViewById(R.id.log_pr);
        googleSigninBtn = view.findViewById(R.id.googleSigninBtn);


        //initialize firebase
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("118043791300-q8fq7d3fds2979ea4k3qe38jvt61oufc.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(getActivity() , gso);

        //handle clicks of login btn
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //check Internet
                if(!Utils.isConnectedToInternet(getContext())){
                    Toast.makeText(getContext(), "Check your Internet connection", Toast.LENGTH_SHORT).show();
                    return;
                }

                //check if data entered is correct
                if (!validateEmail() | !validatePassword()) {
                    return;
                }

                //get email and password
                String email = emailET.getEditText().getText().toString();
                String password = passwordET.getEditText().getText().toString();

                login(email,password);
            }
        });

        //handle clicks of google sign in btn
        googleSigninBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check Internet
                if(!Utils.isConnectedToInternet(getContext())){
                    Toast.makeText(getContext(), "Check your Internet connection", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

        //handle clicks of forget password text view
        forgetPassTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRecoverPasswordDialog();
            }
        });


        return view;
    }

    //Login method
    public void login(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);//to disable user interaction

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    progressBar.setVisibility(View.GONE);
                    getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    Toast.makeText(getActivity(), "Welcome " + mAuth.getCurrentUser().getEmail(), Toast.LENGTH_SHORT).show();
                    MainActivity.refreshMainActivity = true;//to refresh the MainActivity
                    getActivity().finish();
                } else {
                    Toast.makeText(getActivity(), "" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                    getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
            }
        });
    }


    //display dialog for user email recovery
    private void showRecoverPasswordDialog() {
        //build alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Recover Password");

        //set layout LinearLayout
        LinearLayout linearLayout = new LinearLayout(getContext());

        //views to set in dialog
        final EditText mEmail = new EditText(getContext());
        mEmail.setHint("email..");
        mEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        //set the min width of editText to sit a text of M letters regardless of an actual text extension and text size
        mEmail.setMinEms(16);

        //add et to linear layout, and add them to alert dialog
        linearLayout.addView(mEmail);
        linearLayout.setPadding(10,10,10,10);
        builder.setView(linearLayout);

        //button recover
        builder.setPositiveButton("Recover", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //check Internet
                if(!Utils.isConnectedToInternet(getContext())){
                    Toast.makeText(getContext(), "Check your Internet connection", Toast.LENGTH_SHORT).show();
                    return;
                }

                //input data
                String email = mEmail.getText().toString();
                if(email.isEmpty())
                    Toast.makeText(getContext(), "Email cannot be empty", Toast.LENGTH_SHORT).show();
                else
                    beginRecover(email.trim());

            }
        });

        //button cancel
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        //show dialog
        builder.create().show();
    }

    //recover password
    private void beginRecover(String email) {

        //show progress bar
        progressBar.setVisibility(View.VISIBLE);
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);//to disable user interaction

        //send email
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                //email sent successfully
                progressBar.setVisibility(View.GONE);
                getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                if(task.isSuccessful()){
                    Toast.makeText(getActivity(), "Email sent", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getActivity(), "Fail, please try again later", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //email sent fail
                progressBar.setVisibility(View.GONE);
                getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                Toast.makeText(getActivity(), ""+e.getMessage(), Toast.LENGTH_LONG).show();
                Log.d("LoginFragment", "onFailure: " + e.getMessage());
            }
        });
    }


    //Validation methods
    private boolean validateEmail() {
        String email = emailET.getEditText().getText().toString().trim();

        if (email.isEmpty()) {
            emailET.setError("Field can not be empty!");
            return false;

        } else {
            emailET.setError(null);
            return true;
        }
    }

    private boolean validatePassword() {
        String password = passwordET.getEditText().getText().toString().trim();

        if (password.isEmpty()) {
            passwordET.setError("Field can not be empty!");
            return false;
        } else {
            passwordET.setError(null);
            return true;
        }
    }

    //results from google dialog here
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Toast.makeText(getActivity(), "please check your connection" , Toast.LENGTH_SHORT).show();
                Log.d("LoginFragment", "onFailure(Google signin): " + e.getMessage());
            }
        }
    }

    //sign in with google
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        //if user is signing first time, then get and upload user info from google account to firestroe
                        if(task.getResult().getAdditionalUserInfo().isNewUser()){
                            //get user data
                            FirebaseUser user = mAuth.getCurrentUser();
                            String uid = "" + user.getUid();
                            String email = "" + user.getEmail();
                            String phone = "" + user.getPhoneNumber();
                            String name = "" + user.getDisplayName();
                            //if phone is null, set it empty
                            if(phone.equalsIgnoreCase("null")){
                                phone = "";
                            }

                            //create user map
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("id", uid);
                            userMap.put("name", name);
                            userMap.put("email", email);
                            userMap.put("phone", phone);
                            userMap.put("major", "");
                            userMap.put("showPhone", true);
                            userMap.put("showEmail", true);
                            userMap.put("img_url", "default");

                            //upload to firestore
                            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                            firestore.collection("Users").document(mAuth.getCurrentUser().getUid()).set(userMap)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //data uploaded successfully to firestore
                                    Toast.makeText(getActivity(), "Your Account Created Successfully", Toast.LENGTH_LONG).show();
                                    MainActivity.refreshMainActivity = true;//to refresh the MainActivity
                                    getActivity().finish();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    //error when uploading data to firestore
                                    Toast.makeText(getActivity(), "Something went wrong\n  please try again later" , Toast.LENGTH_LONG).show();
                                    Log.d("LoginFragment", "onFailure: " + e.getMessage());
                                }
                            });
                        }else{
                            //otherwise, finish the activity
                            Toast.makeText(getActivity(), "Welcome " + mAuth.getCurrentUser().getEmail(), Toast.LENGTH_SHORT).show();
                            MainActivity.refreshMainActivity = true;//to refresh the MainActivity
                            getActivity().finish();
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(getActivity(), ""+task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
    }

}
