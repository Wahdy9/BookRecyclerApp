package com.example.bookrecycler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class Contac_Us extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contac__us);

        //toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.contact_us);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //init views
        TextView linked_inKhamis = findViewById(R.id.linkin_TV);
        TextView email_inKhamis = findViewById(R.id.email_TV);
        TextView linked_inKhalid =findViewById(R.id.linkin_TV2);
        TextView email_inKhalid = findViewById(R.id.email_TV2);

        //click listeners
        linked_inKhamis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Contac_Us.this, "Link to Khamis LinkedIn", Toast.LENGTH_SHORT).show();
            }
        });

        email_inKhamis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendToEmail("kwahde@stu.kau.edu.sa");
            }
        });

        linked_inKhalid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Contac_Us.this, "Link to Khalid LinkedIn", Toast.LENGTH_SHORT).show();
            }
        });

        email_inKhalid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendToEmail("kwahde@stu.kau.edu.sa");
            }
        });

    }

    //send to email app with the address
    private void sendToEmail(String email){
        String [] addresses = {email};//address to send email to

        //show what email app to open
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        //to make sure app for this intent exist, so it won't crash
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
}
