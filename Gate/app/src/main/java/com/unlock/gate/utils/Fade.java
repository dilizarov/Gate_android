package com.unlock.gate.utils;

import android.animation.Animator;
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

    public static void hide(final View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.animate().setDuration(DURATION);
            view.animate().alpha(0f).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);

                    view.setAlpha(1f);
                    view.setVisibility(View.INVISIBLE);
                    view.animate().setListener(null);
                }
            });
        }
    }

	public static void hide(final View view, AnimatorListenerAdapter listener) {
		if (view.getVisibility() == View.VISIBLE) {
			view.animate().setDuration(DURATION);

            final Animator.AnimatorListener compositeListener = new CompositeAnimatorListenerAdapter(listener) {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setAlpha(1f);
                    view.setVisibility(View.INVISIBLE);
                    view.animate().setListener(null);

                    super.onAnimationEnd(animation);
                }
            };

			view.animate().alpha(0f).setListener(compositeListener);
		}
	}

    /*
    Required to inject our own behavior into listeners passed in.
     */
    private static class CompositeAnimatorListenerAdapter implements Animator.AnimatorListener {
        private final Animator.AnimatorListener mDelegate;

        public CompositeAnimatorListenerAdapter(Animator.AnimatorListener delegate) {
            mDelegate = delegate;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            if (mDelegate != null) {
                mDelegate.onAnimationStart(animation);
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mDelegate != null) {
                mDelegate.onAnimationEnd(animation);
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            if (mDelegate != null) {
                mDelegate.onAnimationCancel(animation);
            }
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
            if (mDelegate != null) {
                mDelegate.onAnimationRepeat(animation);
            }
        }
    }

}
