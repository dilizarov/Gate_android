package com.unlock.gate.utils;

import android.content.Context;
import android.util.AttributeSet;

import com.gc.materialdesign.views.ButtonFloat;

/**
 * Created by davidilizarov on 2/22/15.
 */
public class MyButtonFloat extends ButtonFloat {

    public MyButtonFloat(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        setAttributes(attrs);
    }
}
