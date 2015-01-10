package com.unlock.gate.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * The purpose of this is so that when one presses the Back
 * key of an EditText, it will lose its focus. To work most optimally and expected
 * you will want the XMLs top-level layout to be focusable & focusableInTouchMode.
 *
 * Created by davidilizarov on 12/20/14.
 */
public class CustomEditText extends EditText {

    public CustomEditText(Context context) {
        super(context);
    }

    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK &&
            event.getAction() == KeyEvent.ACTION_UP) {
            clearFocus();

            return false;
        }

        return super.dispatchKeyEvent(event);
    }
}
