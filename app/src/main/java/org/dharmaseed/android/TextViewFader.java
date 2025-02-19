package org.dharmaseed.android;

import android.animation.ObjectAnimator;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;



public class TextViewFader {
    /*
    TextView that fades in when the label changes and fades out
    again when the label remains unchanged for a certain period
     */

    private static final String LOG_TAG = "TextViewFader";

    protected boolean hidden, initialised;
    protected TextView view;
    private Handler handler;

    public TextViewFader(TextView view) {
        handler = new Handler();
        this.view = view;
        reset();
    }

    public void reset() {
        view.setAlpha(0);
        initialised = false;
        hidden = true;
    }

    public void setText(CharSequence text) {
        if (view.getText().equals(text))
            return;

        view.setText(text);
        if (initialised) {
            if (hidden)
                show();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (view.getText().equals(text))
                        hide();
                }
            }, 1000);
        } else {
            initialised = true;
        }
    }

    protected void show() {
        Log.d(LOG_TAG, "starting fade in");
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(
                view, "alpha", view.getAlpha(), 1f
        );
        fadeIn.setDuration(300);
        fadeIn.setAutoCancel(true);
        fadeIn.start();
        hidden = false;
    }

    protected void hide() {
        Log.d(LOG_TAG, "starting fade out");
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(
                view, "alpha", view.getAlpha(), 0f
        );
        fadeOut.setDuration(600);
        fadeOut.setAutoCancel(true);
        fadeOut.start();
        hidden = true;
    }
}
