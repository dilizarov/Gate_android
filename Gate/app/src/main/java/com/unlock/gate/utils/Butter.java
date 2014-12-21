package com.unlock.gate.utils;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

/**
 * Created by davidilizarov on 12/19/14.
 * Toast is always better with butter
 */
public class Butter {

    private static Toast oldToast;
    private static long timeOfOld;

    public static void up(Context context, String message) {
        cancelPrevious();

        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 0);
        toast.show();

        setPrevious(toast);
    }

    public static void down(Context context, String message) {
        cancelPrevious();

        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();

        setPrevious(toast);
    }

    public static void between(Context context, String message) {
        cancelPrevious();

        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        setPrevious(toast);
    }

    public static void downUnlessButtered(Context context, String message) {
        if (oldToast == null || (timeOfOld + 4000 < System.currentTimeMillis()))
            down(context, message);
    }

    public static void cancelPrevious() {
        if (oldToast != null) oldToast.cancel();
    }

    private static void setPrevious(Toast toast) {
        oldToast = toast;
        timeOfOld = System.currentTimeMillis();
    }
}
