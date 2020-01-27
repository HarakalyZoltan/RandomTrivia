package com.example.trivia;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trivia.controller.AppController;
import com.example.trivia.data.AnswerListAsyncResponse;
import com.example.trivia.data.QuestionBank;
import com.example.trivia.model.Question;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

//Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon"> www.flaticon.com</a>

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView questionTextview;
    private TextView livesTextView;
    private TextView comboTextView;
    private Button trueButton;
    private Button falseButton;
    private int currentQuestionIndex = 0;
    private List<Question> questionList;
    private int maxScore=0;
    private int score = 0;
    private int combo = 0;
    private int lives = 3;
    private int maxCombo = 0;
    private int difficulty = 0;
    private double difficultyMultiplier = 1.0;
    private long time = 0;
    private TextView currentScore;
    private TextView topScore;
    private ImageButton shareButton;
    private Boolean switchPref;
    private Boolean vibratorPref;
    private Vibrator vibrator;

    //Timer
    private CountDownTimer countDownTimer;
    private TextView countDownTextView;

    //Popup
    private PopupWindow popUp = null;
    private Button restartButton;
    private Button exitMenuButton;
    private ImageButton watchAdButton;
    private TextView adText;
    private TextView popupComboTextView;
    private TextView popupfinalScoreTextView;

    //Ad player
    private RewardedAd rewardedAd;
    private Boolean rewardEarned = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        trueButton = findViewById(R.id.true_button);
        falseButton = findViewById(R.id.false_button);
        livesTextView = findViewById(R.id.lives_text);
        questionTextview = findViewById(R.id.question_textview);
        currentScore = findViewById(R.id.current_score);
        topScore = findViewById(R.id.top_score);
        comboTextView = findViewById(R.id.combo_text);
        countDownTextView = findViewById(R.id.timer_textView);

        trueButton.setOnClickListener(this);
        falseButton.setOnClickListener(this);

        difficulty = getIntent().getIntExtra("difficulty", 0);
        Log.d("difficulty", "onCreate: " + difficulty);

        setDifficulty(difficulty);
        startTimer();

        SharedPreferences getSharedData = getSharedPreferences("nope", MODE_PRIVATE);
        maxScore = getSharedData.getInt("score", 0);
        topScore.setText("Top score: " + maxScore);

        questionList = new QuestionBank().getQuestions(new AnswerListAsyncResponse() {
            @Override
            public void processFinished(ArrayList<Question> questionArrayList) {
                currentQuestionIndex = getRandomNumber(0, questionArrayList.size());
                questionTextview.setText(questionArrayList.get(currentQuestionIndex).getAnswer());

            }
        });
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        switchPref = sharedPreferences.getBoolean("sound", false);
        vibratorPref = sharedPreferences.getBoolean("vibrate", false);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        assert vibrator != null;

        rewardedAd = createAndLoadRewardedAd();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.true_button:
                checkAnswer(true);
                break;
            case R.id.false_button:
                checkAnswer(false);
                break;

            case R.id.share_button:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT,
                        "I just player Random Trivia and \n" +
                        "my Top score is : " + maxScore +
                                "\n And my Current score is : " + score );
                intent.putExtra(Intent.EXTRA_SUBJECT, "Try to beat my score !!!");
                startActivity(intent);
                break;

            case R.id.back_to_menu_button:
                popUp.dismiss();
                startActivity(new Intent(this, MenuActivity.class));
                finish();
                break;

            case R.id.restart_button:
                SharedPreferences sharedPreferences = getSharedPreferences("nope", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("index", currentQuestionIndex);
                int max = sharedPreferences.getInt("score", 0);

                if (score > max) {
                    editor.putInt("score", score);
                }
                editor.apply();

                setDifficulty(difficulty);
                startTimer();
                score = 0;
                combo = 0;
                currentQuestionIndex = getRandomNumber(0, questionList.size());
                updateQuestion();
                popUp.dismiss();
                break;

            case R.id.watch_ad_button:
                if (rewardedAd.isLoaded()) {
                    Activity activityContext = MainActivity.this;
                    RewardedAdCallback adCallback = new RewardedAdCallback() {
                        @Override
                        public void onUserEarnedReward(@NonNull RewardItem reward) {
                            // User earned reward.
                            rewardEarned = true;
                            lives++;
                            updateQuestion();
                        }

                        @Override
                        public void onRewardedAdClosed() {
                            // Ad closed.
                            if (rewardEarned) {
                                startTimer();
                                popUp.dismiss();
                                rewardEarned = false;
                            }
                            rewardedAd = createAndLoadRewardedAd();

                        }

                        @Override
                        public void onRewardedAdFailedToShow(int errorCode) {
                            // Ad failed to display.
                            Toast.makeText(MainActivity.this, "Something went wrong.", Toast.LENGTH_SHORT).show();
                        }
                    };
                    rewardedAd.show(activityContext, adCallback);
                } else {
                    Log.d("adLoader", "The rewarded ad wasn't loaded yet.");
                }
                break;
        }
    }

    private void updateQuestion() {
        questionTextview.setText(questionList.get(currentQuestionIndex).getAnswer());
        currentScore.setText("Current score: " + score);
        comboTextView.setText("Combo: " + combo);
        livesTextView.setText("Lives: " + lives);
    }

    private void checkAnswer(Boolean answer) {
        boolean answerIsTrue = questionList.get(currentQuestionIndex).getAnswerTrue();
        if (answer == answerIsTrue) {
            if (switchPref){
                MediaPlayer correctMp = MediaPlayer.create(this, R.raw.correct);
                correctMp.start();
            }
            if (vibratorPref){
                vibrate();
            }
            stopTimer();
            startTimer();


            fadeView();
            combo += 1;
            score += 10 * combo * difficultyMultiplier;
            currentQuestionIndex = (getRandomNumber(0, questionList.size()));
            updateQuestion();
        }
        else {
            if (lives == 1) {
                if (switchPref) {
                    MediaPlayer gameOverMp = MediaPlayer.create(this, R.raw.game_over);
                    gameOverMp.start();
                }
                if (vibratorPref){
                    vibrate();
                }

                stopTimer();
                lives--;
                createPopup();
            }else {
                if (switchPref) {
                    MediaPlayer wrongMp = MediaPlayer.create(this, R.raw.wrong);
                    wrongMp.start();
                }
                if (vibratorPref){
                    vibrate();
                }

                stopTimer();
                startTimer();
                shakeAnimation();
                if (combo > maxCombo)
                    maxCombo = combo;
                combo = 0;
                lives--;

                currentQuestionIndex = (getRandomNumber(0, questionList.size()));
                updateQuestion();
            }
        }
    }

    private void fadeView() {
        final CardView cardView = findViewById(R.id.cardView);
        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
        alphaAnimation.setDuration(350);
        alphaAnimation.setRepeatCount(1);
        alphaAnimation.setRepeatMode(Animation.REVERSE);
        cardView.setAnimation(alphaAnimation);

        alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                cardView.setCardBackgroundColor(Color.GREEN);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                cardView.setCardBackgroundColor(Color.WHITE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

    }
    private void shakeAnimation() {
        Animation shake = AnimationUtils.loadAnimation(MainActivity.this,
                R.anim.shake_animation);

        final CardView cardView = findViewById(R.id.cardView);
        cardView.setAnimation(shake);

        shake.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                cardView.setCardBackgroundColor(Color.RED);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                cardView.setCardBackgroundColor(Color.WHITE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = getSharedPreferences("nope", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("index", currentQuestionIndex);
        int max = sharedPreferences.getInt("score", 0);

        if (score > max) {
            editor.putInt("score", score);
        }
        editor.apply();
    }

    public static int getRandomNumber(int min, int max){
        Random random = new Random();
        return random.nextInt((max - min) + 1) + min;
    }

    private void createPopup() {
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        // Inflate the popup_layout.xml
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert layoutInflater != null;
        View layout = layoutInflater.inflate(R.layout.popup, null);

        // Creating the PopupWindow
        popUp = new PopupWindow(this);
        popUp.setContentView(layout);
        popUp.setWidth(width);
        popUp.setHeight(height);
        popUp.setFocusable(false);
        popUp.setOutsideTouchable(false);

        restartButton = layout.findViewById(R.id.restart_button);
        exitMenuButton = layout.findViewById(R.id.back_to_menu_button);
        shareButton = layout.findViewById(R.id.share_button);
        popupfinalScoreTextView = layout.findViewById(R.id.popup_final_score_txt);
        popupComboTextView = layout.findViewById(R.id.popup_combo_txt);
        watchAdButton = layout.findViewById(R.id.watch_ad_button);
        adText = layout.findViewById(R.id.ad_text);

        restartButton.setOnClickListener(this);
        exitMenuButton.setOnClickListener(this);
        shareButton.setOnClickListener(this);
        watchAdButton.setOnClickListener(this);

        popupComboTextView.setText("Max Combo: " + maxCombo);
        popupfinalScoreTextView.setText("Final score: " + score);

        // prevent clickable background
        popUp.setBackgroundDrawable(null);
        popUp.showAtLocation(layout, Gravity.CENTER, 1, 1);
    }

    private void vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            vibrator.vibrate(500);
        }
    }

    private void setDifficulty(int difficulty) {
        switch (difficulty) {
            case 0:
                difficultyMultiplier = 1.0;
                lives = 10;
                time = 31000;
                break;
            case 1:
                difficultyMultiplier = 1.5;
                lives = 5;
                time = 21000;
                break;
            case 2:
                difficultyMultiplier = 2.0;
                lives = 3;
                time = 11000;
                break;
        }
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(time, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countDownTextView.setText("" + millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                countDownTextView.setText("0");
                if (switchPref) {
                    MediaPlayer gameOverMp = MediaPlayer.create(MainActivity.this, R.raw.game_over);
                    gameOverMp.start();
                }
                if (vibratorPref){
                    vibrate();
                }
                createPopup();
            }
        }.start();
    }

    private void stopTimer() {
        countDownTimer.cancel();
    }

    public RewardedAd createAndLoadRewardedAd() {
        RewardedAd rewardedAd = new RewardedAd(this,
                "ca-app-pub-3940256099942544/5224354917");
        RewardedAdLoadCallback adLoadCallback = new RewardedAdLoadCallback() {
            @Override
            public void onRewardedAdLoaded() {
                // Ad successfully loaded.
            }

            @Override
            public void onRewardedAdFailedToLoad(int errorCode) {
                // Ad failed to load.
            }
        };
        rewardedAd.loadAd(new AdRequest.Builder().build(), adLoadCallback);
        return rewardedAd;
    }


}
