package com.example.hotelease;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ImageView imageView = findViewById(R.id.splashImage);

        // Load animation
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        imageView.startAnimation(anim);

        // Move to login after animation
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, login.class);
            startActivity(intent);
            finish();
        }, 2000);
    }
}
