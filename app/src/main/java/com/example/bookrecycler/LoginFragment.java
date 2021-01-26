package com.example.bookrecycler;


import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.text.InputType;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

//TODO: implement google signin + redesign
public class LoginFragment extends Fragment {

    //views
    private TextInputLayout emailET, passwordET;
    private Button loginBtn;
    private TextView forgetPassTV;
    private ProgressBar progressBar;

    //Firebase
    private FirebaseAuth mAuth;

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

        //initialize firebase
        mAuth = FirebaseAuth.getInstance();

        //handle clicks of login btn
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
    private void login(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);//to disable user interaction

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    progressBar.setVisibility(View.GONE);
                    getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    Toast.makeText(getActivity(), "Logged as " + mAuth.getCurrentUser().getEmail(), Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                } else {
                    Toast.makeText(getActivity(), "Error, please try later " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
            }
        });
    }


    //display dialog for user email
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
                //input data
                String email = mEmail.getText().toString().trim();
                beginRecover(email);
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
                    Toast.makeText(getActivity(), "Fail..", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //email sent fail
                progressBar.setVisibility(View.GONE);
                getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
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
            // emailET.setErrorEnabled(false);//to remove the space for error
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

}
