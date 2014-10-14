package com.unlock.gate.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class SetErrorBugFixer implements TextWatcher {

	private EditText view;
	
	public SetErrorBugFixer(EditText view) {
		this.view = view;
	}
	
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		if (view.getError() != null) view.setError(null);
	}

	@Override
	public void afterTextChanged(Editable s) {
		// TODO Auto-generated method stub

	}

}
