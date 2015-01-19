package com.unlock.gate;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import com.unlock.gate.utils.Butter;
import com.unlock.gate.utils.CustomEditText;


public class EnterKeyActivity extends Activity {

    private CustomEditText keyText;
    private int beforeLength;

    private TextWatcher keyTextWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_key);

        final Intent intent = getIntent();

        keyText = (CustomEditText) findViewById(R.id.key_text);
        keyText.requestFocus();

        keyTextWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                beforeLength = s.length();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(final Editable s) {
                String plausableKey = s.toString().replaceAll("-", "");

                if (plausableKey.length() == 16) {
                    intent.putExtra("key", plausableKey);
                    setResult(RESULT_OK, intent);

                    finish();
                } else if ((s.length() >= beforeLength) && (s.length() == 4 || s.length() == 9 || s.length() == 14)) {
                    keyText.removeTextChangedListener(keyTextWatcher);
                    keyText.setText(s.toString() + "-");
                    keyText.post(new Runnable() {
                        @Override
                        public void run() {
                            keyText.setSelection(s.length() + 1);
                        }
                    });

                    keyText.addTextChangedListener(keyTextWatcher);
                } else if ( s.length() < beforeLength && (s.length() == 4 || s.length() == 9 || s.length() == 14)) {
                    keyText.removeTextChangedListener(keyTextWatcher);
                    keyText.setText(s.subSequence(0, s.length() - 1));
                    keyText.post(new Runnable() {
                        @Override
                        public void run() {
                            keyText.setSelection(s.length() - 1);
                        }
                    });

                    keyText.addTextChangedListener(keyTextWatcher);
                }
            }
        };

        keyText.addTextChangedListener(keyTextWatcher);

        if (intent.getStringExtra("errorMessage") != null) {
            Butter.between(this, intent.getStringExtra("errorMessage"));
        }
    }
}
