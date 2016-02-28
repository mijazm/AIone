package com.mijaz.project.aione;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.widget.ImageView;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView splash_image = (ImageView)findViewById(R.id.splash_image);

        AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(this,R.animator.flip);

        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

                Intent i = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(i);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        set.setTarget(splash_image);

        set.setDuration(3000);

        set.start();

    }
}
