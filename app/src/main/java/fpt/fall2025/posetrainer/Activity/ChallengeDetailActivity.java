package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import fpt.fall2025.posetrainer.R;

public class ChallengeDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.challenge_detail);

        String bodyPart = getIntent().getStringExtra("body_part");
        if (bodyPart != null) {
            TextView tvBodyPart = findViewById(R.id.tv_title);
            tvBodyPart.setText(bodyPart);
        }
        CardView btnStart = findViewById(R.id.cStartButton);
        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(this, WorkoutActivity.class);
            startActivity(intent);
        });

    }
}