package com.unlock.gate.utils;

import android.animation.AnimatorListenerAdapter;
import android.view.View;

//Simple utility for fading view elements while also handling visibility

public class Fade {

	private static long DURATION = 250;
	
	public static void setDuration(long duration) {
		if (duration >= 0) {
			DURATION = duration;
		}
	}
	
	public static void show(View view) {
		if (view.getVisibility() != View.VISIBLE) {
			view.setAlpha(0);
			view.setVisibility(View.VISIBLE);
			view.animate().setDuration(DURATION);
			view.animate().alpha(1);
		}
	}
	
	public static void show(View view, AnimatorListenerAdapter listener) {
		if (view.getVisibility() != View.VISIBLE) {
			view.setAlpha(0);
			view.setVisibility(View.VISIBLE);
			view.animate().setDuration(DURATION);
			view.animate().alpha(1).setListener(listener);
		}
	}

	// the listener passed in must at some point do:
	//view.setAlpha(1f);
	//view.setVisibility(View.INVISIBLE);
	//view.animate().setListener(null);
	//Note, you would call view whatever it is referenced as in the context.
	//Yes, this is hacky. I'll think of a better solution later.
	public static void hide(View view, AnimatorListenerAdapter listener) {
		if (view.getVisibility() == View.VISIBLE) {
			view.animate().setDuration(DURATION);
			view.animate().alpha(0f).setListener(listener);
		}
	}
}
