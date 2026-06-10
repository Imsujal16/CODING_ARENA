package com.arena.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.arena.app.utils.ClerkAuthHelper;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Warm up auth in background
        new ClerkAuthHelper(this);

        LottieAnimationView lottieView = findViewById(R.id.lottie_splash);
        TextView textArena = findViewById(R.id.text_arena_logo);
        TextView textTagline = findViewById(R.id.text_tagline);

        // Start at invisible
        textArena.setAlpha(0f);
        textTagline.setAlpha(0f);
        lottieView.setAlpha(0f);
        lottieView.setScaleX(0.7f);
        lottieView.setScaleY(0.7f);

        // Play ONCE, not looping
        lottieView.setRepeatCount(0);
        lottieView.playAnimation();

        // Lottie fade-in + scale-up (cinematic entrance)
        ObjectAnimator lottieAlpha = ObjectAnimator.ofFloat(lottieView, View.ALPHA, 0f, 1f);
        lottieAlpha.setDuration(500);

        ObjectAnimator lottieScaleX = ObjectAnimator.ofFloat(lottieView, View.SCALE_X, 0.7f, 1f);
        ObjectAnimator lottieScaleY = ObjectAnimator.ofFloat(lottieView, View.SCALE_Y, 0.7f, 1f);
        lottieScaleX.setDuration(700);
        lottieScaleY.setDuration(700);
        lottieScaleX.setInterpolator(new OvershootInterpolator(1.2f));
        lottieScaleY.setInterpolator(new OvershootInterpolator(1.2f));

        AnimatorSet lottieEntrance = new AnimatorSet();
        lottieEntrance.playTogether(lottieAlpha, lottieScaleX, lottieScaleY);
        lottieEntrance.start();

        // ARENA title fades in after lottie entrance (delayed)
        ObjectAnimator arenaFadeIn = ObjectAnimator.ofFloat(textArena, View.ALPHA, 0f, 1f);
        arenaFadeIn.setDuration(900);
        arenaFadeIn.setStartDelay(600);
        arenaFadeIn.setInterpolator(new DecelerateInterpolator());

        // Slide up ARENA text
        textArena.setTranslationY(30f);
        ObjectAnimator arenaSlideUp = ObjectAnimator.ofFloat(textArena, View.TRANSLATION_Y, 30f, 0f);
        arenaSlideUp.setDuration(900);
        arenaSlideUp.setStartDelay(600);
        arenaSlideUp.setInterpolator(new DecelerateInterpolator());

        // Tagline fades in last
        ObjectAnimator taglineFadeIn = ObjectAnimator.ofFloat(textTagline, View.ALPHA, 0f, 1f);
        taglineFadeIn.setDuration(700);
        taglineFadeIn.setStartDelay(1100);
        taglineFadeIn.setInterpolator(new AccelerateDecelerateInterpolator());

        // Start text animations
        AnimatorSet textAnimations = new AnimatorSet();
        textAnimations.playTogether(arenaFadeIn, arenaSlideUp, taglineFadeIn);
        textAnimations.start();

        // Listen for Lottie completion → fade out everything → launch MainActivity
        lottieView.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Short hold, then fade everything out gracefully
                lottieView.postDelayed(() -> {
                    ObjectAnimator fadeOutAll = ObjectAnimator.ofFloat(
                            findViewById(android.R.id.content), View.ALPHA, 1f, 0f);
                    fadeOutAll.setDuration(500);
                    fadeOutAll.setInterpolator(new AccelerateDecelerateInterpolator());
                    fadeOutAll.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                            overridePendingTransition(android.R.anim.fade_in, 0);
                            finish();
                        }
                    });
                    fadeOutAll.start();
                }, 400); // brief hold after lottie ends
            }
        });
    }
}
