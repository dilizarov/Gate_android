package com.unlock.gate.utils;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by davidilizarov on 11/13/14.
 */
public class NfcUtils {

    public static NdefMessage stringsToNdefMessage(String... strings) {
        ArrayList<NdefRecord> ndefRecords = new ArrayList<NdefRecord>();

        for (String string : strings) {
            byte[] textBytes = string.getBytes();
            NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
                    "text/plain".getBytes(), new byte[] {}, textBytes);

            ndefRecords.add(textRecord);
        }

        ndefRecords.add(NdefRecord.createApplicationRecord("com.unlock.gate"));

        NdefRecord[] records = new NdefRecord[ndefRecords.size()];
        ndefRecords.toArray(records);
        return new NdefMessage(records);
    }

    public static ArrayList<String> getNdefMessagePayload(NdefMessage message) {
        NdefRecord[] records = message.getRecords();

        ArrayList<String> payload = new ArrayList<String>();

        for (NdefRecord record : records) payload.add(new String(record.getPayload()));
        return payload;
    }

    public static NdefMessage[] getNdefMessages(Intent intent) {

        Log.v("Yo", "we're in");

        NdefMessage[] messages = null;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            Parcelable[] rawMessages = intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES
            );

            if (rawMessages != null) {
                messages = new NdefMessage[rawMessages.length];
                int len = rawMessages.length;
                for (int i = 0; i < len; i++) {
                    messages[i] = (NdefMessage) rawMessages[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[]{};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN,
                        empty, empty, empty);

                NdefMessage message = new NdefMessage(new NdefRecord[]{
                        record
                });

                messages = new NdefMessage[]{
                        message
                };
            }
        }

        return messages;
    }

}
