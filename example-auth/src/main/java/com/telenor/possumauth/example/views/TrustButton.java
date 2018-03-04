package com.telenor.possumauth.example.views;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.telenor.possumauth.example.AppConstants;
import com.telenor.possumauth.example.Messaging;
import com.telenor.possumauth.example.Send;

import java.util.Locale;

public class TrustButton extends RelativeLayout {
    private TrustWheel trustWheel;
    private TextView centerTextView;
    private boolean authenticating;
    private float trustScore;
    private int timePassedInMillis;
    private Handler authHandler = new Handler(Looper.getMainLooper());
    private Runnable authRunnable;

    public TrustButton(Context context) {
        super(context);
        init(context);
    }

    public TrustButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TrustButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void createRunnable() {
        authRunnable = () -> {
            timePassedInMillis += authInterval();
            if (timePassedInMillis >= authTime()) {
                timePassedInMillis = 0;
                Send.message(getContext(), Messaging.AUTH_VERIFY);
            } else {
                authHandler.postDelayed(authRunnable, authInterval());
            }
            trustWheel.setProgress(timePassedInMillis);
        };
    }

    private void init(Context context) {
        trustWheel = new TrustWheel(context, authTime());
        addView(trustWheel);
        centerTextView = new TextView(context);
        centerTextView.setTextColor(Color.GRAY);
        centerTextView.setTextSize(pixelValue(10)); // TODO: Text size should be size dependent

        centerTextView.setBackgroundColor(Color.TRANSPARENT);
        LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        centerTextView.setLayoutParams(params);
        addView(centerTextView);
        centerTextView.bringToFront();
        setTrustScore(trustScore, null);
    }

    private float pixelValue(int dpValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, getResources().getDisplayMetrics());
    }


    /**
     * Time spent waiting for an authentication
     *
     * @return the time in milliseconds
     */
    private int authTime() {
        return AppConstants.AUTHENTICATION_TIME;
    }

    private int authInterval() {
        return 20;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        trustWheel.setEnabled(enabled);
    }

    public void setTrustScore(float score, String overridingStatus) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Invalid trustScore, must be within 0 and 100. Was " + score + ".");
        }
        if (centerTextView == null || trustWheel == null) return;
        if (overridingStatus != null) {
            centerTextView.setTextColor(Color.WHITE);
            centerTextView.setText(overridingStatus);
        } else {
            centerTextView.setText(String.format(Locale.US, "%.0f%%", score));
            centerTextView.setTextColor(Color.BLACK);
        }
        trustScore = score;
        trustWheel.setTrustScore(score);
    }

    public void authenticate() {
        createRunnable();
        timePassedInMillis = 0;
        trustWheel.setProgress(0);
        authenticating = true;
        authHandler.removeCallbacks(authRunnable);
        authHandler.postDelayed(authRunnable, authInterval());
    }

    public void stopAuthenticate() {
        if (authenticating) {
            timePassedInMillis = 0;
            trustWheel.setProgress(0);
            authHandler.removeCallbacks(authRunnable);
            authenticating = false;
        }
    }
}