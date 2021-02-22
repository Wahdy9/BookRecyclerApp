package com.example.bookrecycler;

import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.OnBackPressedDispatcherOwner;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ComponentActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class Contac_Us extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contac__us);

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
        TextView linked_inKhamis = findViewById(R.id.linkin_TV);
        TextView email_inKhamis = findViewById(R.id.email_TV);
        TextView linked_inKhalid =findViewById(R.id.linkin_TV2);
        TextView email_inKhalid = findViewById(R.id.email_TV2);

        linked_inKhamis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Contac_Us.this, "Link to Khamis LinkedIn", Toast.LENGTH_SHORT).show();
            }
        });

        email_inKhamis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Contac_Us.this, "Email to Khamis ", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(Contac_Us.this, "Link to Khalid Email", Toast.LENGTH_SHORT).show();
            }
        });



    }
}