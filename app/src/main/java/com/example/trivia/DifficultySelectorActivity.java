package com.example.trivia;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class DifficultySelectorActivity extends AppCompatActivity implements View.OnClickListener {
    private Button easyButton;
    private Button mediumButton;
    private Button hardButton;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_difficulty_selector);

        easyButton = findViewById(R.id.easy_button);
        mediumButton = findViewById(R.id.medium_button);
        hardButton = findViewById(R.id.hard_button);
        backButton = findViewById(R.id.back_button);


        easyButton.setOnClickListener(this);
        mediumButton.setOnClickListener(this);
        hardButton.setOnClickListener(this);
        backButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.easy_button:
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("difficulty", 0);
                startActivity(intent);
                break;
            case R.id.medium_button:
                Intent intent1 = new Intent(this, MainActivity.class);
                intent1.putExtra("difficulty", 1);
                startActivity(intent1);
                break;
            case R.id.hard_button:
                Intent intent2 = new Intent(this, MainActivity.class);
                intent2.putExtra("difficulty", 2);
                startActivity(intent2);
                break;
            case R.id.back_button:
                startActivity(new Intent(this, MenuActivity.class));
                finish();
                break;
        }
    }
}
