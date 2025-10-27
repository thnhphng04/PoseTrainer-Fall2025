package fpt.fall2025.posetrainer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

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

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            finish();
        });


        ImageView btnMenu = findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(this, v);
            popupMenu.getMenuInflater().inflate(R.menu.menu_settings_exercise, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_restart_plan) {
                    Toast.makeText(this, "Restart plan clicked", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.menu_adjust_plan) {
                    Toast.makeText(this, "Adjust plan clicked", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });


            popupMenu.show();
        });
    }
}