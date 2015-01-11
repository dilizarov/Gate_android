package com.unlock.gate;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.unlock.gate.utils.Butter;
import com.unlock.gate.utils.CustomEditText;


public class CreateGateActivity extends Activity {

    private ImageButton createGate;
    private CustomEditText createGateName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_gate);

        final Intent intent = getIntent();

        createGate     = (ImageButton) findViewById(R.id.createGate);
        createGateName = (CustomEditText) findViewById(R.id.createGateName);
        createGateName.requestFocus();

        if (!(createGateName.getText().toString().trim().length() > 0))
            createGate.setEnabled(false);


        createGateName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().length() > 0) createGate.setEnabled(true);
                else createGate.setEnabled(false);
            }
        });

        createGate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.putExtra("gateName", createGateName.getText().toString().trim());
                setResult(RESULT_OK, intent);

                finish();

            }
        });

        if (intent.getStringExtra("gateName") != null) createGateName.append(intent.getStringExtra("gateName"));

        if (intent.getStringExtra("errorMessage") != null) {
            Butter.between(this, intent.getStringExtra("errorMessage"));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_create_gate, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
