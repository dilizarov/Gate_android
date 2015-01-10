package com.unlock.gate.utils;

import android.text.TextUtils;

public class CustomValidator {
	
	public static boolean isValidEmail(CharSequence target) {
		return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
	}
	
}
